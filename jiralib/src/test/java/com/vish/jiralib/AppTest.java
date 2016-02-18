package com.vish.jiralib;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.testng.AssertJUnit;
import org.testng.Reporter;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.atlassian.jira.rest.client.api.domain.BasicProject;

public class AppTest {
	
	private Jira jira;
	private List<BasicProject> list;
	private String jiraproject;
	@BeforeTest
	public void initializeJira() throws Exception {
		Properties props = new Properties();
		props.load(new FileInputStream(new File(System.getProperty("user.dir") + File.separator + "jira.properties")));
		String jiraurl = props.getProperty("jiraurl");
		String jirausername = props.getProperty("jirausername");
		String jirapassword = props.getProperty("jirapwd");
		jiraproject = props.getProperty("jiraproject");
		if (jiraurl == null || jirausername == null || jirapassword == null)
			throw new Exception ("tests require a jira.properties file with the following properties: jiraurl, jirausername, jirapwd, jiraproject");
		
		jira = new Jira(jiraurl, jirausername, jirapassword, jiraproject,false);
	}
	
	@Test(enabled=true,priority=1,description="check count of projects")
	public void testAssertProjectCountIsNonZero() throws Exception {
		list = jira.getProjects();
		AssertJUnit.assertTrue("project count is zero", list.size()>0);
	}
	
	@Test(enabled=false,priority=2, description="run JQL")
	public void testRunJQL() throws Exception {
		String proj = list.get(0).getKey();
		String jql = "project = " + proj + " and created > startOfMonth()"; 
		Reporter.log("JQL:" + jql, Level.INFO.intValue(),true);
		AssertJUnit.assertTrue("zero issues found!", jira.getIssuesByJQL(jql).size() > 0);
	}
	
	@Test(enabled=false,priority=3, description="run JQL and perform bulk-transitions on the result")
	public void testBulkTransitionByJQL() throws Exception {
		String proj = list.get(0).getKey();
		String jql = "project = " + proj + " and key = " + proj + "-4";
		jira.bulkTransitionIssuesByJQL(jql, "Done");
	}
}
