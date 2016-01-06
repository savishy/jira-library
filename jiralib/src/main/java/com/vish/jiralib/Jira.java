package com.vish.jiralib;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.BasicStatus;
import com.atlassian.jira.rest.client.api.domain.Filter;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.api.domain.util.ErrorCollection;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

/**
 * a common JIRA Rest client. works in any situation where JIRA needs to be used programmatically.
 * <p>
 * References:
 * <ul>
 * <li><a href="https://ecosystem.atlassian.net/wiki/display/JRJC/Home">Home Page</a></li>
 * <li><a href="https://docs.atlassian.com/jira-rest-java-client-api/2.0.0-m31/jira-rest-java-client-api/apidocs/">Javadoc</a></li> 
 * </ul>
 * @author vish
 *
 */
public class Jira {
	boolean DEBUG = true;
	/** Jira Rest Client */
	public static JiraRestClient restClient;
	/** valid issuetypes */
	public List<IssueType> issueTypes = new ArrayList<IssueType>();
	/** valid components */
	public List<BasicComponent> components = new ArrayList<BasicComponent>();
	/** current project */
	public Project project;
	
	/**
	 * constructor. initialize JIRA REST Client. 
	 * <p>
	 * updates current project. gets latest list of components and issuetypes.
	 * @param url
	 * @param u
	 * @param p
	 * @throws Exception 
	 */
	public Jira(String url, String u, String p, String proj) throws Exception {
		AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		URI jiraServerUri = null;
		try {
			jiraServerUri = new URI(url);
			restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, u, p);
			//initialize current project
			project = restClient.getProjectClient().getProject(proj).claim();
			//update issuetypes and components
			updateProjectComponents();
			updateProjectIssueTypes();
		} catch (URISyntaxException e) {
			System.err.println("ERR:" + e.getMessage());
		}
	}
	
	public List<BasicProject> getProjects() throws Exception {
		Iterator<BasicProject> iter = restClient.getProjectClient().getAllProjects().claim().iterator();
		List<BasicProject> retVal = new ArrayList<BasicProject>();
		while (iter.hasNext()) {
			retVal.add(iter.next());
		}
		return retVal;
	}

	/**
	 * parse JIRA Rest errors and get error message and status. Then fail with Exception.
	 * 
	 * @param e use method {@link RestClientException#getErrorCollections()}
	 */
	private void parseJiraRestError(Collection<ErrorCollection> e) throws Exception {
		Iterator<ErrorCollection> iter = e.iterator();
		String msg = "REST Exception:";
		while (iter.hasNext()) {
			ErrorCollection coll = iter.next();
			msg += " (" + coll.getStatus() + ")";
			msg += " " + coll.getErrorMessages() + " ";
		}
		throw new Exception(msg);
	}
	
	/**
	 * Retrieve issue using its key
	 * @param key a valid JIRA issue key. 
	 * @return {@code Map<String,String>} containing following keys:<br>
	 * key - issue ID<br>
	 * summary - issue summary<br>
	 * description<br>
	 * status - status of issue<br>
	 * @throws Exception
	 */
	public Map<String,String> getIssueByKey(String key) throws Exception {
		System.out.println("getIssue() " + key);
		Map<String,String> issueFields = new HashMap<String,String>();
		try {
			Issue issue = getIssueObjectByName(key);
			issueFields.put("key",issue.getKey());
			issueFields.put("summary",issue.getSummary());
			issueFields.put("description",issue.getDescription());
			BasicStatus status = issue.getStatus();
			issueFields.put("status",status.toString());
			System.out.println(issueFields.toString());
			return issueFields;
		} catch (RestClientException e) {
			parseJiraRestError(e.getErrorCollections());
			throw new Exception ("error fetching issue");
		}
	}
	

	/**
	 * feed in a valid JQL and get issues as a Map.
	 * @param jql
	 * @return {@code Map<String,String>} containing following keys:<br>
	 * key - issue ID<br>
	 * values:String[summary,description,status]<br>
	 * summary - issue summary<br>
	 * description<br>
	 * status - status of issue<br>
	 * @throws Exception
	 */
	public Map<String,String[]> getIssuesByJQL(String jql) throws Exception {
		List<Issue> issues = getIssueObjectsByJQL(jql);
		Map<String,String[]> issueFields = new HashMap<String,String[]>();

		for (Issue issue : issues) {
			BasicStatus status = issue.getStatus();
			issueFields.put(issue.getKey(), new String[]{
					issue.getSummary(),
					issue.getDescription(),
					status.toString()
						});
		}
		return issueFields;
	}
	
	/**
	 * return issue objects as a List. Search by JQL.
	 * @param jql
	 * @return
	 * @throws Exception
	 */
	public List<Issue> getIssueObjectsByJQL(String jql) throws Exception {
		List<Issue> retVal = new ArrayList<Issue>();
		try {
			if (DEBUG) System.out.println("JQL:" + jql);
			SearchResult result = restClient.getSearchClient().searchJql(jql).claim();
			if (DEBUG) System.out.println(result.getTotal() + " results");
			if (result.getTotal() == 0) return retVal;
			if (result.getTotal() > result.getMaxResults())
				System.out.println("WARNING: " + result.getTotal() + " results, printing only first " + result.getMaxResults());
			Iterator<Issue> iter = result.getIssues().iterator();
			while (iter.hasNext()) {
				retVal.add(iter.next());
			}
			return retVal;
		} catch (RestClientException e) {
			parseJiraRestError(e.getErrorCollections());
			return null;
		}
	}
	
	/**
	 * Private method. Called by constructor.
	 * @throws Exception
	 */
	private void updateProjectIssueTypes() throws Exception {
		Iterator<IssueType> iter = project.getIssueTypes().iterator();
		while (iter.hasNext()) {
			issueTypes.add(iter.next());
		}
	}
	
	/**
	 * Private method. Called by constructor.
	 * @throws Exception
	 */
	private void updateProjectComponents() throws Exception {
		Iterator<BasicComponent> iter = project.getComponents().iterator();
		while (iter.hasNext()) {
			components.add(iter.next());
		}
	}
	
	/**
	 * do a bulk-transition on issues found by filter Id.
	 * @param filterId
	 * @param targetState
	 * @throws Exception
	 */
	public void bulkTransitionIssuesByFilterId(long filterId, String targetState) throws Exception {
		List<Issue> issues = getIssueObjectsByFilterId(filterId);
		doBulkTransition(issues, targetState);
	}
	
	/**
	 * perform a bulk transition on issues found by JQL.
	 * @param jql
	 * @param targetState a valid target state. e.g. "Done". Case insensitive.
	 * @throws Exception
	 */
	public void bulkTransitionIssuesByJQL(String jql, String targetState) throws Exception {
		List<Issue> issues = getIssueObjectsByJQL(jql);
		doBulkTransition(issues, targetState);
	}
	
	private void doBulkTransition(List<Issue> issues, String targetState) throws Exception {
		//check validity of transitions only for first issue
		//get transition info for first issue. 
		int transitionId = -1;
		if (issues.size() > 0) { 
			List<Transition> trans = getTransitions(issues.get(0));
			transitionId = getTransitionIdByName(issues.get(0), targetState);
		}
		//loop through issues and transition them.
		for (Issue issue : issues) {
			if (DEBUG) System.out.println("targetState:" + targetState);
			transitionIssue(issue,transitionId);
		}

	}
	
	/**
	 * run a filter ID and get issue objects. 
	 * @param filterId e.g. in {@code https://myjira.atlassian.net/issues/?filter=11200} the filter ID is 11200.
	 * @return
	 * @throws Exception
	 */
	public List<Issue> getIssueObjectsByFilterId(long filterId) throws Exception {
		System.out.println("Getting issues using filter-id: " + filterId);
		String jql = null;
		try {
			Filter filter = restClient.getSearchClient().getFilter(filterId).claim();
			jql = filter.getJql();			
		} catch (RestClientException e) {
			parseJiraRestError(e.getErrorCollections()); 
		}
		return getIssueObjectsByJQL(jql);
	}
	
	
	/**
	 * transition an issue to another state. Get the transition id using {@link #getTransitionIdByName(Issue, String)}
	 * @param issue
	 * @param transitionId
	 * @throws Exception
	 */
	public void transitionIssue(Issue issue,int transitionId) throws Exception {
		String status = issue.getStatus().getName();
		if (DEBUG) System.out.println(issue.getKey() + ": " + status + " > " + transitionId);
		TransitionInput transition = new TransitionInput(transitionId);
		restClient.getIssueClient().transition(issue, transition).claim();
	}
	
	/**
	 * get list of transitions for an issue.
	 * @param issue
	 * @return
	 * @throws Exception
	 */
	private List<Transition> getTransitions(Issue issue) throws Exception {
		Iterator<Transition> iter = restClient.getIssueClient().getTransitions(issue).claim().iterator();
		List<Transition> retVal = new ArrayList<Transition>();
		while (iter.hasNext()) {
			retVal.add(iter.next());
		}
		return retVal;
	}
	
	/**
	 * given transition name, get its ID.
	 * This method will automatically fail if the transition is invalid. 
	 * @param issue
	 * @param name
	 * @return
	 * @throws Exception
	 */
	private int getTransitionIdByName(Issue issue,String name) throws Exception {
		if (DEBUG) System.out.println("getTransitionIdByName: transition: " + name);
		Iterator<Transition> iter = restClient.getIssueClient().getTransitions(issue).get().iterator();
		while (iter.hasNext()) {
			Transition t = iter.next();
			if (DEBUG) System.out.println("transition:" + t);
			if (t.getName().toLowerCase().equals(name.toLowerCase())) {
				return t.getId();
			}
		}
		throw new Exception ("transition " + name + " not found! Check whether your JIRA account has required permissions.");
	}
	
	/**
	 * get {@link IssueType} object from string.
	 * @param p {@link Project} object referencing current JIRA project.
	 * @param issueType the issue-type as a string e.g "Bug". Case-insensitive.
	 * @return
	 * @throws Exception
	 */
	private IssueType getIssueTypeByName(Project p, String issueType) throws Exception {
		for (IssueType it : issueTypes) {
			if (it.getName().toLowerCase().equals(issueType.toLowerCase())) {
				return it;
			}
		}
		return (IssueType) null;
	}
	
	private BasicComponent getComponentByName(Project p, String comp) throws Exception {
		for (BasicComponent it : components) {
			if (it.getName().toLowerCase().equals(comp.toLowerCase())) {
				return it;
			}
		}
		return (BasicComponent) null;
	}
	
	/**
	 * Private method. Given issue name (i.e issue Key) return an Issue object.
	 * @param key
	 * @return
	 * @throws Exception
	 */
	private Issue getIssueObjectByName(String key) throws Exception {
		return restClient.getIssueClient().getIssue(key).claim();
	}

	
	/**
	 * Create issue in the current project. 
	 * @param issueType a valid issue type. 
	 * @param component valid component. 
	 * @param summary issue summary
	 * @param description issue description
	 * @throws Exception
	 */
	public void createIssue(String issueType, 
							String component,
							String summary,
							String description) throws Exception {
		System.out.println("createIssue(): " + project + " " + issueType);
		IssueType it = getIssueTypeByName(project, issueType);
		BasicComponent comp = getComponentByName(project, component);
		
		//error handling
		if (it == null) throw new Exception ("invalid issue type: " + issueType + 
				". valid types are:" + Arrays.toString(issueTypes.toArray()));
		if (comp == null) throw new Exception ("invalid component: " + component + 
				". valid values are:" + Arrays.toString(components.toArray()));
		
		IssueInputBuilder builder = new IssueInputBuilder(project, it);
		IssueInput issueInput = builder	
									.setComponents(comp)
									.setDescription(description)
									.setSummary(summary)
									.build()
										;
		BasicIssue issue = restClient.getIssueClient().createIssue(issueInput).claim();
		System.out.println(issue.getKey() + " created");		
	}
}
