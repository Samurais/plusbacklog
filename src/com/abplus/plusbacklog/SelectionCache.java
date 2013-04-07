package com.abplus.plusbacklog;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/04/06 13:52
 */
public class SelectionCache {
    private BacklogIO backlogIO;
    private LayoutInflater inflater;
    private List<Project> projects = null;

    private final String DEBUG_TAG = "+backlog.selection_cache";

    public SelectionCache(Activity activity, BacklogIO io) {
        inflater = activity.getLayoutInflater();
        backlogIO = io;
    }

    public void loadProjects(final BacklogIO.ResponseNotify notify) {
        final ProjectParser parser = new ProjectParser();

        backlogIO.loadProjects(new BacklogIO.ResponseNotify() {
            @Override
            public void success(int code, String response) {
                try {
                    parser.parse(response);
                    projects = parser.getProjects();
                    notify.success(code, response);
                } catch (XmlPullParserException e) {
                    notify.error(e);
                } catch (IOException e) {
                    notify.error(e);
                }
            }

            @Override
            public void failed(int code, String response) {
                notify.failed(code, response);
            }

            @Override
            public void error(Exception e) {
                notify.error(e);
            }
        });
    }

    public void loadIssueTypes(final Project current, final BacklogIO.ResponseNotify notify) {
        final IssueTypeParser parser = new IssueTypeParser(current);

        backlogIO.loadIssueTypes(current.getId(), new BacklogIO.ResponseNotify() {
            @Override
            public void success(int code, String response) {
                try {
                    parser.parse(response);
                    notify.success(code, response);
                } catch (XmlPullParserException e) {
                    current.issueTypes = null;
                    notify.error(e);
                } catch (IOException e) {
                    current.issueTypes = null;
                    notify.error(e);
                }
            }

            @Override
            public void failed(int code, String response) {
                notify.failed(code, response);
            }

            @Override
            public void error(Exception e) {
                notify.error(e);
            }
        });
    }

    public BaseAdapter getProjectsAdapter() {
        return new ProjectsAdapter();
    }

    public BaseAdapter getIssueTypeAdapter(Project current) {
        return new IssueTypeAdapter(current);
    }

    private abstract class StructParser {

        void parse(String source) throws XmlPullParserException, IOException {
            XmlPullParser xpp = Xml.newPullParser();
            xpp.setInput(new StringReader(source));

            for (int et = xpp.getEventType(); et != XmlPullParser.END_DOCUMENT; et = xpp.next()) {
                if (et == XmlPullParser.START_DOCUMENT) {
                    Log.d(DEBUG_TAG, "Document start.");
                } else if (et == XmlPullParser.START_TAG) {
                    Log.d(DEBUG_TAG, "Start tag " + xpp.getName());
                    if (xpp.getName().equals("struct")) {
                        parseStruct(xpp);
                    }
                } else if (et == XmlPullParser.END_TAG) {
                    Log.d(DEBUG_TAG, "End tag " + xpp.getName());
                } else if (et == XmlPullParser.TEXT) {
                    Log.d(DEBUG_TAG, "Text " + xpp.getText());
                }
            }
            Log.d(DEBUG_TAG, "Document end.");
        }

        void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {

            for (int et = xpp.next(); et != XmlPullParser.END_DOCUMENT; et = xpp.next()) {
                if (et == XmlPullParser.START_TAG) {
                    Log.d(DEBUG_TAG + ".parseStruct", "Start tag " + xpp.getName());
                    if (xpp.getName().equals("member")) {
                        parseMember(xpp);
                    }
                } else if (et == XmlPullParser.END_TAG) {
                    Log.d(DEBUG_TAG + ".parseStruct", "End tag " + xpp.getName());
                    if (xpp.getName().equals("struct")) break;
                } else if (et == XmlPullParser.TEXT) {
                    Log.d(DEBUG_TAG + ".parseStruct", "Text " + xpp.getText());
                }
            }
        }

        void parseMember(XmlPullParser xpp) throws IOException, XmlPullParserException {
            String tag = "";
            String name = null;

            for (int et = xpp.next(); et != XmlPullParser.END_DOCUMENT; et = xpp.next()) {
                if (et == XmlPullParser.START_TAG) {
                    Log.d(DEBUG_TAG + ".parseMember", "Start tag " + xpp.getName());
                    tag = xpp.getName();
                    if (tag.equals("value")) {
                        parseValue(name, xpp);
                    }
                } else if (et == XmlPullParser.END_TAG) {
                    Log.d(DEBUG_TAG + ".parseMember", "End tag " + xpp.getName());
                    if (xpp.getName().equals("member")) break;
                } else if (et == XmlPullParser.TEXT) {
                    Log.d(DEBUG_TAG + ".parseMember", "Text " + xpp.getText());
                    if (tag.equals("name")) {
                        name = xpp.getText();
                    }
                }
            }
        }

        abstract void parseValue(String name, XmlPullParser xpp) throws IOException, XmlPullParserException;
    }

    private class ProjectParser extends StructParser {
        List<Project> projects = new ArrayList<Project>();
        Project project = null;

        @Override
        void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {
            project = new Project();
            super.parseStruct(xpp);
            projects.add(project);
            project = null;
        }

        @Override
        void parseValue(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {

            for (int et = xpp.next(); et != XmlPullParser.END_DOCUMENT; et = xpp.next()) {
                if (et == XmlPullParser.START_TAG) {
                    Log.d(DEBUG_TAG + ".parseValue", "Start tag " + xpp.getName());
                } else if (et == XmlPullParser.END_TAG) {
                    Log.d(DEBUG_TAG + ".parseValue", "End tag " + xpp.getName());
                    if (xpp.getName().equals("value")) break;
                } else if (et == XmlPullParser.TEXT) {
                    Log.d(DEBUG_TAG + ".parseValue", "Text " + xpp.getText());
                    if (project != null) {
                        if (name.equals("id")) {
                            project.id = Integer.parseInt(xpp.getText());
                        } else if (name.equals("name")) {
                            project.name = xpp.getText();
                        } else if (name.equals("key")) {
                            project.key = xpp.getText();
                        } else if (name.equals("url")) {
                            project.url = xpp.getText();
                        } else if (name.equals("archived")) {
                            project.archived = xpp.getText().equals("1");
                        }
                    }
                }
            }
        }

        public List<Project> getProjects() {
            return projects;
        }
    }

    private class IssueTypeParser extends StructParser {
        Project project;
        IssueType issueType = null;

        IssueTypeParser(Project project) {
            super();
            this.project = project;
        }

        @Override
        void parse(String source) throws XmlPullParserException, IOException {
            project.issueTypes = new ArrayList<IssueType>();
            super.parse(source);
        }

        @Override
        void parseStruct(XmlPullParser xpp) throws IOException, XmlPullParserException {
            issueType = new IssueType();
            super.parseStruct(xpp);
            project.issueTypes.add(issueType);
            issueType = null;
        }

        @Override
        void parseValue(String name, XmlPullParser xpp) throws IOException, XmlPullParserException {

            for (int et = xpp.next(); et != XmlPullParser.END_DOCUMENT; et = xpp.next()) {
                if (et == XmlPullParser.START_TAG) {
                    Log.d(DEBUG_TAG + ".parseValue", "Start tag " + xpp.getName());
                } else if (et == XmlPullParser.END_TAG) {
                    Log.d(DEBUG_TAG + ".parseValue", "End tag " + xpp.getName());
                    if (xpp.getName().equals("value")) break;
                } else if (et == XmlPullParser.TEXT) {
                    Log.d(DEBUG_TAG + ".parseValue", "Text " + xpp.getText());
                    if (project != null) {
                        if (name.equals("id")) {
                            issueType.id = Integer.parseInt(xpp.getText());
                        } else if (name.equals("name")) {
                            issueType.name = xpp.getText();
                        } else if (name.equals("color")) {
                            issueType.color = xpp.getText();
                        }
                    }
                }
            }
        }
    }

    public class IssueType {
        private int id;
        private String name;
        private String color;

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public int getColor() {
            String c = color.substring(1, color.length());
            return Integer.parseInt(c, 16) + 0xFF000000;
        }
    }

    public class Project {
        private List<IssueType> issueTypes = null;
        private int id;
        private String name;
        private String key;
        private String url;
        private boolean archived;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public boolean isArchived() {
            return archived;
        }

        public boolean hasCache() {
            return issueTypes != null;
        }
    }

    class ProjectsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return projects.size();
        }

        @Override
        public Object getItem(int position) {
            return projects.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout result = (LinearLayout)convertView;

            if (result == null) {
                result = (LinearLayout)inflater.inflate(R.layout.project_item, null);
            }
            Project project = projects.get(position);
            TextView keyView = (TextView)result.findViewById(R.id.project_key);
            TextView nameView = (TextView)result.findViewById(R.id.project_name);
            TextView urlView = (TextView)result.findViewById(R.id.project_url);

            keyView.setText(project.getKey());
            nameView.setText(project.getName());

            String url = project.getUrl();
            if (url == null || url.isEmpty()) {
                urlView.setVisibility(View.GONE);
            } else {
                urlView.setVisibility(View.VISIBLE);
                urlView.setText(url);
            }

            if (project.isArchived()) {
                keyView.setTextColor(Color.LTGRAY);
            } else {
                keyView.setTextColor(Color.BLACK);
            }

            result.setTag(project);

            return result;
        }
    }

    class IssueTypeAdapter extends BaseAdapter {
        private Project project;

        IssueTypeAdapter(Project project) {
            super();

            this.project = project;
        }

        @Override
        public int getCount() {
            return project.issueTypes.size();
        }

        @Override
        public Object getItem(int position) {
            return project.issueTypes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView result = (TextView)convertView;

            if (result == null) {
                result = (TextView)inflater.inflate(R.layout.spinner_item, null);
            }
            IssueType issueType = project.issueTypes.get(position);
            result.setText(issueType.getName());
            result.setBackgroundColor(issueType.getColor());
            result.setTextColor(Color.WHITE);
            result.setTag(issueType);

            return result;
        }
    }
}