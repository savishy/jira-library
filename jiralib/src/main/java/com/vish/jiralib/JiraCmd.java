package com.vish.jiralib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.atlassian.jira.rest.client.api.domain.Issue;

import bsh.StringUtil;

public class JiraCmd {

	private Jira jira;
	private String jiraproject, searchInput, targetState;
	private actions action;
	private static enum actions {
		bulk,
		find
	}
	public JiraCmd(String[] args) throws Exception {
		Properties props = new Properties();
		props.load(new FileInputStream(new File(System.getProperty("user.dir") + File.separator + "jira.properties")));
		String jiraurl = props.getProperty("jiraurl");
		String jirausername = props.getProperty("jirausername");
		String jirapassword = props.getProperty("jirapwd");
		if (jiraurl == null || jirausername == null || jirapassword == null)
			throw new Exception ("we require a jira.properties file with the following properties: jiraurl, jirausername, jirapwd, jiraproject");
		
		//parse input args
		parseArgs(args);
		jira = new Jira(jiraurl, jirausername, jirapassword, jiraproject, true);
	}

	private void bulkAction(String searchInput, String targetState) throws Exception {
		boolean byFilter = false;
		try {
			Long.parseLong(searchInput);
			byFilter = true;
		} catch (NumberFormatException e) {
			byFilter = false;
		}
		if (byFilter)
			jira.bulkTransitionIssuesByFilterId(Long.parseLong(searchInput), targetState);
		else
			jira.bulkTransitionIssuesByJQL(searchInput, targetState);
	}
	
	public void findAction(String searchInput) throws Exception {
		boolean byFilter = false;
		try {
			Long.parseLong(searchInput);
			byFilter = true;
		} catch (NumberFormatException e) {
			byFilter = false;
		}
		List<Issue> issues = new ArrayList<Issue>();
		if (byFilter) { 
			issues = jira.getIssueObjectsByFilterId(Long.parseLong(searchInput));
		}
		else {
			issues = jira.getIssueObjectsByJQL(searchInput);			
		}
		//print and exit
		String delim = "\t\t";
		System.out.println("KEY" + delim + "SUMMARY" + delim + "STATUS" + delim);
		for (Issue issue : issues) {
			int charlimit = 40;
			String sum = issue.getSummary().length() > charlimit ? issue.getSummary().substring(0, charlimit)  + "..." : issue.getSummary();
			System.out.println(	issue.getKey() + delim + 
								sum + delim +
								issue.getStatus().getName() + delim);
		}
	}
	
	private void usage() {
		System.out.println(
		"-----\n" +
		"USAGE:\n" + 
		"-----\n" +
		"java -jar JiraCmd.jar [ACTION] [PROJECT] [ADDITIONAL ARGS]\n\n" +
		"ACTION: can be one of " + Arrays.asList(actions.values()) + "\n" + 
		"PROJECT: a valid project key, e.g. TEST\n" +
		"ADDITIONAL ARGS: additional arguments based on the ACTION.\n\n" + 
		"Examples:\n" +
		"1. Do Bulk Actions on Issues in a JIRA Filter\n" +
		"java -jar JiraCmd.jar bulk TEST 11300 Done\n" +
		"- runs the filter-ID 11300 and gets issue list. Issues in this filter must be part of project key \"TEST\"\n" + 
		"- for each issue, changes state to \"Done\". This must be a valid state for the issue.\n" +
		"\n\n" +
		"2. Find issues by JQL\n" + 
		"java -jar JiraCmd.jar find TEST jql-here\n\n"
				);		
	}
	private void parseArgs(String[] args) throws Exception {
		if (args.length < 2) { usage(); throw new Exception ("incorrect argument count!"); }
		
		//first argument has to be a word
		action = actions.valueOf(args[0]);
		//second argument has to be the project.
		jiraproject = args[1];
		
		switch (action) {
		case bulk: 
			if (args.length != 4) { usage(); throw new Exception ("incorrect argument count!"); } 
			searchInput = args[2];
			targetState = args[3]; 
			break;
		case find:
			if (args.length != 3) { usage(); throw new Exception ("incorrect argument count!"); } 
			searchInput = args[2];
			break;
		default: throw new Exception (action + " unsupported!");
		}	
	}
	
	private void doAction() throws Exception {
		switch (action) {
		case bulk: 
			bulkAction(searchInput,targetState); break;
		case find:
			findAction(searchInput); break;
		default: throw new Exception (action + " unsupported!");
		}
	}
	
	public static void main(String[] args) {
		try {
			JiraCmd jiraCmd = new JiraCmd(args);
			jiraCmd.doAction();
			System.exit(0);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}
