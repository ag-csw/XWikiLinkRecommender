package de.csw.xwiki;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.internal.XWikiCachingRightService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;


/**
 * This class should only serve as a temporary solution. It works around the
 * fact that XWiki 5.x does not provide any way to add mappings of custom
 * actions to permission types (the mapping is hard-coded into the rights
 * service implementation).
 * 
 * As soon as this bug is solved, this class should not be used any more. 
 * 
 * @author ralph
 * @deprecated
 */
public class CSWXWikiCachingRightService implements XWikiRightService {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(XWikiCachingRightService.class);
	private static final String DELETE_ACTION = "delete";
	private static final String LOGIN_ACTION = "login";
	private static final ActionMap ACTION_MAP = new ActionMap();
	private DocumentReferenceResolver<String> documentReferenceResolver;
	private DocumentReferenceResolver<String> userAndGroupReferenceResolver;
	private final AuthorizationManager authorizationManager;

	public CSWXWikiCachingRightService() {
		this.documentReferenceResolver = ((DocumentReferenceResolver) Utils
				.getComponent(DocumentReferenceResolver.TYPE_STRING,
						"currentmixed"));

		this.userAndGroupReferenceResolver = ((DocumentReferenceResolver) Utils
				.getComponent(DocumentReferenceResolver.TYPE_STRING, "user"));

		this.authorizationManager = ((AuthorizationManager) Utils
				.getComponent(AuthorizationManager.class));
	}

	public static Right actionToRight(String action) {
		Right right = (Right) ACTION_MAP.get(action);
		if (right == null) {
			return Right.ILLEGAL;
		}
		return right;
	}

	private DocumentReference resolveUserName(String username,
			WikiReference wikiReference) {
		return this.userAndGroupReferenceResolver.resolve(username,
				new Object[] { wikiReference });
	}

	private DocumentReference resolveDocumentName(String docname,
			WikiReference wikiReference) {
		return this.documentReferenceResolver.resolve(docname,
				new Object[] { wikiReference });
	}

	private void showLogin(XWikiContext context) {
		try {
			if ((context.getRequest() != null)
					&& (!("login".equals(context.getAction())))
					&& (!(context.getWiki().Param("xwiki.hidelogin", "false")
							.equalsIgnoreCase("true")))) {
				context.getWiki().getAuthService().showLogin(context);
			}
		} catch (XWikiException e) {
			LOGGER.error("Failed to show login page.", e);
		}
	}

	private DocumentReference getCurrentUser(XWikiContext context) {
		DocumentReference contextUserReference = context.getUserReference();
		DocumentReference userReference = contextUserReference;

		if ((userReference == null) && (context.getMode() != 2)) {
			try {
				XWikiUser user = context.getWiki().checkAuth(context);
				if (user != null)
					userReference = resolveUserName(user.getUser(),
							new WikiReference(context.getDatabase()));
			} catch (XWikiException e) {
				LOGGER.error("Caught exception while authenticating user.", e);
			}
		}

		if ((userReference != null)
				&& ("XWikiGuest".equals(userReference.getName()))) {
			userReference = null;
		}

		if ((userReference != contextUserReference)
				&& (((userReference == null) || (!(userReference
						.equals(contextUserReference)))))) {
			context.setUserReference(userReference);
		}

		return userReference;
	}

	private Boolean checkNeedsAuthValue(String value) {
		if ((value != null) && (!(value.equals("")))) {
			if (value.toLowerCase().equals("yes"))
				return Boolean.valueOf(true);
			try {
				if (Integer.parseInt(value) > 0)
					return Boolean.valueOf(true);
			} catch (NumberFormatException e) {
				LOGGER.warn(
						"Failed to parse the authenticate_* preference value [{}]",
						value);
			}
		}
		return null;
	}

	private boolean needsAuth(Right right, XWikiContext context) {
		String prefName = "authenticate_" + right.getName();

		String value = context.getWiki().getXWikiPreference(prefName, "",
				context);
		Boolean result = checkNeedsAuthValue(value);
		if (result != null) {
			return result.booleanValue();
		}

		value = context.getWiki().getSpacePreference(prefName, "", context)
				.toLowerCase();
		result = checkNeedsAuthValue(value);
		if (result != null) {
			return result.booleanValue();
		}

		return false;
	}

	public boolean checkAccess(String action, XWikiDocument doc,
			XWikiContext context) throws XWikiException {
		Right right = actionToRight(action);
		EntityReference entityReference = doc.getDocumentReference();

		LOGGER.debug("checkAccess for action [{}] on entity [{}].", right,
				entityReference);

		DocumentReference userReference = getCurrentUser(context);

		if ((userReference == null) && (needsAuth(right, context))) {
			showLogin(context);
			return false;
		}

		if (this.authorizationManager.hasAccess(right, userReference,
				entityReference)) {
			return true;
		}

		if ((userReference == null) && (!("delete".equals(action)))
				&& (!("login".equals(action)))) {
			LOGGER.debug(
					"Redirecting unauthenticated user to login, since it have been denied [{}] on [{}].",
					right, entityReference);

			showLogin(context);
		}

		return false;
	}

	public boolean hasAccessLevel(String rightName, String username,
			String docname, XWikiContext context) throws XWikiException {
		WikiReference wikiReference = new WikiReference(context.getDatabase());
		DocumentReference document = resolveDocumentName(docname, wikiReference);
		LOGGER.debug(
				"hasAccessLevel() resolved document named [{}] into reference [{}]",
				docname, document);
		DocumentReference user = resolveUserName(username, wikiReference);

		if ("XWikiGuest".equals(user.getName())) {
			user = null;
		}

		Right right = Right.toRight(rightName);

		return ((((user != null) || (!(needsAuth(right, context))))) && (this.authorizationManager
				.hasAccess(right, user, document)));
	}

	public boolean hasProgrammingRights(XWikiContext context) {
		if (context.hasDroppedPermissions()) {
			return false;
		}
		XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
		return hasProgrammingRights((sdoc != null) ? sdoc : context.getDoc(),
				context);
	}

	public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context) {
		WikiReference wiki;
		DocumentReference user;

		if (doc != null) {
			user = doc.getContentAuthorReference();
			wiki = doc.getDocumentReference().getWikiReference();
		} else {
			user = context.getUserReference();
			wiki = new WikiReference(context.getDatabase());
		}

		if ((user != null) && ("XWikiGuest".equals(user.getName()))) {
			user = null;
		}

		return this.authorizationManager.hasAccess(Right.PROGRAM, user, wiki);
	}

	public boolean hasAdminRights(XWikiContext context) {
		XWikiDocument doc = context.getDoc();
		if (doc == null) {
			return hasWikiAdminRights(context);
		}
		DocumentReference user = context.getUserReference();
		DocumentReference document = doc.getDocumentReference();

		if ((user != null) && ("XWikiGuest".equals(user.getName()))) {
			user = null;
		}

		return this.authorizationManager.hasAccess(Right.ADMIN, user, document);
	}

	public boolean hasWikiAdminRights(XWikiContext context) {
		DocumentReference user = context.getUserReference();
		WikiReference wiki = new WikiReference(context.getDatabase());

		if ((user != null) && ("XWikiGuest".equals(user.getName()))) {
			user = null;
		}

		return this.authorizationManager.hasAccess(Right.ADMIN, user, wiki);
	}

	public List<String> listAllLevels(XWikiContext context)
			throws XWikiException {
		return Right.getAllRightsAsString();
	}

	static {
		ACTION_MAP.putAction("login", Right.LOGIN)
				.putAction("imagecaptcha", Right.LOGIN)
				.putAction("view", Right.VIEW)
				.putAction("delete", Right.DELETE)
				.putAction("distribution", Right.VIEW)
				.putAction("admin", Right.ADMIN)
				.putAction("programing", Right.PROGRAM)
				.putAction("edit", Right.EDIT)
				.putAction("register", Right.REGISTER)
				.putAction("logout", Right.LOGIN)
				.putAction("loginerror", Right.LOGIN)
				.putAction("loginsubmit", Right.LOGIN)
				.putAction("viewrev", Right.VIEW)
				.putAction("viewattachrev", Right.VIEW)
				.putAction("get", Right.VIEW)
				.putAction("downloadrev", Right.VIEW)
				.putAction("plain", Right.VIEW).putAction("raw", Right.VIEW)
				.putAction("attach", Right.VIEW)
				.putAction("charting", Right.VIEW)
				.putAction("skin", Right.VIEW)
				.putAction("download", Right.VIEW).putAction("dot", Right.VIEW)
				.putAction("svg", Right.VIEW).putAction("pdf", Right.VIEW)
				.putAction("undelete", Right.EDIT)
				.putAction("reset", Right.DELETE)
				.putAction("commentadd", Right.COMMENT)
				.putAction("redirect", Right.VIEW)
				.putAction("export", Right.VIEW)
				.putAction("import", Right.ADMIN).putAction("jsx", Right.VIEW)
				.putAction("ssx", Right.VIEW).putAction("tex", Right.VIEW)
				.putAction("unknown", Right.VIEW).putAction("save", Right.EDIT)
				.putAction("preview", Right.EDIT).putAction("lock", Right.EDIT)
				.putAction("cancel", Right.EDIT)
				.putAction("delattachment", Right.EDIT)
				.putAction("inline", Right.EDIT)
				.putAction("propadd", Right.EDIT)
				.putAction("propupdate", Right.EDIT)
				.putAction("propdelete", Right.EDIT)
				.putAction("propdisable", Right.EDIT)
				.putAction("propenable", Right.EDIT)
				.putAction("objectadd", Right.EDIT)
				.putAction("objectremove", Right.EDIT)
				.putAction("objectsync", Right.EDIT)
				.putAction("rollback", Right.EDIT)
				.putAction("upload", Right.EDIT)
				.putAction("create", Right.VIEW)
				.putAction("deleteversions", Right.ADMIN)
				.putAction("deletespace", Right.ADMIN)
				.putAction("temp", Right.VIEW)
				.putAction("cswlinks", Right.VIEW);
	}

	private static class ActionMap extends HashMap<String, Right> {
		private static final long serialVersionUID = 1L;

		public ActionMap putAction(String action, Right right) {
			put(action, right);
			return this;
		}
	}
}