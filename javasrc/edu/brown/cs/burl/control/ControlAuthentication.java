/********************************************************************************/
/*                                                                              */
/*              ControlAuthentication.java                                      */
/*                                                                              */
/*      Login and authentication methods                                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Steven P. Reiss                                          */
/*********************************************************************************
 *                                                                               *
 *  This work is licensed under Creative Commons Attribution-NonCommercial 4.0   *
 *  International.  To view a copy of this license, visit                        *      
 *      https://creativecommons.org/licenses/by-nc/4.0/                          *
 *                                                                               *
 ********************************************************************************/


package edu.brown.cs.burl.control;

import com.sun.net.httpserver.HttpExchange;

import edu.brown.cs.burl.burl.BurlException;
import edu.brown.cs.burl.burl.BurlUser;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.bower.BowerRouter;
import edu.brown.cs.ivy.bower.BowerUtil;
import edu.brown.cs.ivy.file.IvyLog;

class ControlAuthentication implements ControlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ControlStorage  burl_store;
private ControlMain     burl_main;
private ControlSessionStore session_store;

private static boolean EMAIL_VALIDATION = false;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlAuthentication(ControlMain main,ControlSessionStore sstore)
{
   burl_main = main;
   burl_store = burl_main.getStorage();
   session_store = sstore;
}


/********************************************************************************/
/*                                                                              */
/*      Login methods                                                           */
/*                                                                              */
/********************************************************************************/

String handlePreLogin(HttpExchange he,ControlSession session)
{
   String email = BowerRouter.getParameter(he,"email");
   String salt = getSaltFromEmail(email);
   
   return BowerRouter.jsonOKResponse(session,"salt",salt,
         "code",session.getCode());
}


String handlePreRegister(HttpExchange he,ControlSession session)
{
   String salt = BurlUtil.randomString(SALT_LENGTH);
   
   session.setCode(salt);
   session_store.updateSession(session);
   
   return BowerRouter.jsonOKResponse(session,"salt",salt,
         "code",session.getCode());
}


String handlePreChangePassword(HttpExchange he,ControlSession session)
{
   BurlUser bu = session.getUser();
   if (bu == null) {
      return BowerRouter.errorResponse(he,session,402,"No User");
    }
   
   String salt = bu.getSalt();
   return BowerRouter.jsonOKResponse(session,"salt",salt,
         "code",session.getCode());
}



String handleLogin(HttpExchange he,ControlSession session)
{
   String email = BowerRouter.getParameter(he,"email");
   String pwd = BowerRouter.getParameter(he,"password");
   
   if (email == null || email.isEmpty() || pwd == null || pwd.isEmpty()) {
      return BowerRouter.errorResponse(he,session,400,
            "Invalid email or password");
    }
   
   email = email.toLowerCase();
   
   ControlUser user = burl_store.loginUser(email,pwd,session.getCode());
   
   if (user == null) {
      return BowerRouter.errorResponse(he,session,400,
            "Invalid email or password");
    } 
   
   session.setUser(user);
   session_store.updateSession(session);
   
   if (user.isTempUsed()) {
      return BowerRouter.jsonOKResponse(session,"TEMPORARY",true);
    }
   
   return BowerRouter.jsonOKResponse(session);
}


/********************************************************************************/
/*                                                                              */
/*      Registration methods                                                    */
/*                                                                              */
/********************************************************************************/

String handleRegister(HttpExchange he,ControlSession session)
{
   String email = BowerRouter.getParameter(he,"email");
   String pwd = BowerRouter.getParameter(he,"password");
   String salt = BowerRouter.getParameter(he,"salt");
 
   if (salt == null || !salt.equals(session.getCode())) {
      return BowerRouter.errorResponse(he,session,400,
            "Invalid salt");
    }
   String newcode = BurlUtil.randomString(SESSION_CODE_LENGTH);   
   session.setCode(newcode);
   session_store.updateSession(session);
   
   if (email == null || email.isEmpty() || pwd == null || pwd.isEmpty()) {
      return BowerRouter.errorResponse(he,session,400,
            "Imavalid email or password");
    }
   email = email.toLowerCase();
   
// if (!BowerUtil.validateEmail(email)) {
//    return BowerRouter.errorResponse(he,session,400,"Invalid email");
//  }
   
   // validate password?
   
   String checkcode = null;
   if (EMAIL_VALIDATION) {
      checkcode = BurlUtil.randomString(EMAIIL_CODE_LENGTH); 
    }
   
   ControlUser user = null;
   try {
      user = burl_store.registerUser(email,pwd,salt,checkcode);  
    }
   catch (BurlException e) {
      return BowerRouter.errorResponse(he,session,400,e.getMessage());
    }
   if (user == null) {
      return BowerRouter.errorResponse(he,session,400,
            "Problem registering user");
    }
   
   if (checkcode != null) {
      sendRegistrationEmail(he,session,email,checkcode);
      // should handle failure here
    }
   
   return BowerRouter.jsonOKResponse(session);
}



private boolean sendRegistrationEmail(HttpExchange he,ControlSession session,
      String email,String valid)
{
   String pfx = burl_main.getUrlPrefix(); 
   // need to get host from he
   String msg = "Thank you for registering with BURL.\n";
   msg += "To complete the reqistration process, please click on or paste the link:\n";
   msg += "   " + pfx + "/validate?";
   msg += "email=" + BurlUtil.encodeURIComponent(email);
   msg += "&code=" + valid;
   msg += "\n";
   
   IvyLog.logD("BURL","SEND EMAIL to " + email + " " + msg);
   
   boolean sts = BurlUtil.sendEmail(email,"Verify your Email for BURL",msg); 
   
   return sts;
} 



String handleValidationRequest(HttpExchange he,ControlSession session)
{
   String email = BowerRouter.getParameter(he,"email");
   String code = BowerRouter.getParameter(he,"code");
   
   if (code == null || email == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad validation request");
    }
   email = email.toLowerCase();
   
   boolean fg = burl_store.validateUser(email,code);  
   
   if (!fg) {
      return BowerRouter.errorResponse(he,session,400,
            "Outdated or bad validation request");
    }
   
   String result = "<html>" +
   "<p>Thank you for validating your email. </p> " + 
   "<p>You should be able to log into BURL now.</p>" +
   "<p>WELCOME to BURL !!!</p>" +
   "</html>";
   
   return result;
}




/********************************************************************************/
/*                                                                              */
/*      Logout methods                                                          */
/*                                                                              */
/********************************************************************************/

String handleLogout(HttpExchange he,ControlSession session)
{
   session.setUser(null);
   session_store.updateSession(session);
   
   return BowerRouter.jsonOKResponse(session);
}



/********************************************************************************/
/*                                                                              */
/*      Authorization methods                                                   */
/*                                                                              */
/********************************************************************************/

String handleAuthentication(HttpExchange he,ControlSession session)
{
   Number uid = session.getUserId();
   if (uid == null) {
      return BowerRouter.errorResponse(he,session,402,
            "Unauthorized");
    }
   ControlUser user = burl_store.findUserById(uid); 
   session.setUser(user);
   session_store.updateSession(session);
   if (user == null) {
      return BowerRouter.errorResponse(he,session,402,
            "Unauthorized");
    }
   
   return null; 
}



/********************************************************************************/
/*                                                                              */
/*      Password methods                                                        */
/*                                                                              */
/********************************************************************************/

String handleForgotPassword(HttpExchange he,ControlSession session) 
{
   String email = BowerRouter.getParameter(he,"email");
   email = email.toLowerCase();
   if (!BowerUtil.validateEmail(email)) {
      return BowerRouter.errorResponse(he,session,400,"Bad email");
    }
   
   // Code should be 12 long
   // Email should say login with the given code and then change your password.
   // Note that the code is only valid once.
   // compute a temporary password using email and this code
   // update the user's temppassword field to the resul
   
   ControlUser user = burl_store.findUserByEmail(email);
   
   if (user != null) {
      String code = BurlUtil.randomString(PASSWORD_LENGTH);
      String pwd = BurlUtil.secureHash(code);
      String salt = getSaltFromEmail(email);
      pwd = BurlUtil.secureHash(pwd + salt);
      
      burl_store.setTemporaryPassword(user.getId(),pwd);
      String msg = "To log into BURL, please use the one-time password " + code + "\n\n";
      msg += "Once you have logged in with this code, please change your password.\n\n";
      msg += "Thank you for using BURL.\n";
      msg += "\n";
      BurlUtil.sendEmail(email,"Password request for BURL",msg);
    }
   
   return BowerRouter.jsonOKResponse(session);
}


String handleChangePassword(HttpExchange he,ControlSession session)
{
   Number uid = session.getUserId();
   String pwd = BowerRouter.getParameter(he,"userpwd");
   String salt = BowerRouter.getParameter(he,"salt");
   ControlUser user = session.getUser();
   if (user == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad user");
    }
   if (salt == null) {
      salt = user.getSalt();
      pwd = BurlUtil.secureHash(pwd + salt);
    }
   burl_store.updatePassword(uid,pwd);  
   
   return BowerRouter.jsonOKResponse(session);
}



/********************************************************************************/
/*                                                                              */
/*      Remove user methods                                                     */
/*                                                                              */
/********************************************************************************/

String handleRemoveUser(HttpExchange he,ControlSession session)
{
   Number uid = session.getUserId();
   burl_store.removeUser(uid);
   
   return BowerRouter.jsonOKResponse(session);
}



/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

private String getSaltFromEmail(String email)
{
   String salt = "XXXXXXXXXXXXXXXX";
   if (email != null && !email.isEmpty()) {
      email = email.toLowerCase();
      ControlUser user = burl_store.findUserByEmail(email);
      if (user != null) {
         salt = user.getSalt();
       }
      else {
         salt = BurlUtil.secureHash(email);
         salt = salt.substring(0,16);
       }
    }
   
   return salt;
}



}       // end of class ControlAuthentication




/* end of ControlAuthentication.java */

