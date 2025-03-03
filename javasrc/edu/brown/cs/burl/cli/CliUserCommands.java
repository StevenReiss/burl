/********************************************************************************/
/*                                                                              */
/*              CliUserCommands.java                                            */
/*                                                                              */
/*      description of class                                                    */
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


package edu.brown.cs.burl.cli;

import java.io.Console;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.file.IvyLog;

class CliUserCommands implements CliConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CliMain         cli_main;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CliUserCommands(CliMain main)
{
   cli_main = main;
}



/********************************************************************************/
/*                                                                              */
/*      Register Command                                                        */
/*                                                                              */
/********************************************************************************/

void handleRegister(List<String> args)
{
   String email = null;
   String pwd = null;
   boolean save = false;
   
   for (String s : args) {
      if (s.startsWith("-")) {
         if (s.startsWith("-s")) {                      // -save
            save = true;
          }
         else {
            badRegisterArgs();
            return;
          }
       }
      else if (email == null) email = s;
      else if (pwd == null) pwd = s;
      else {
         badRegisterArgs();
         return;
       }
    }
   if (email == null) {
      badRegisterArgs();
      return;
    }
   
   if (pwd == null) {
      pwd = readPassword();
      if (pwd == null) return;
    }
   
   JSONObject pre = cli_main.createHttpGet("register","email",email);
   if (!pre.getString("status").equals("OK")) return;
   
   String salt = pre.optString("salt");
   
   String rpwd0 = BurlUtil.secureHash(pwd);
   String rpwd1 = BurlUtil.secureHash(rpwd0 + salt);
   
   JSONObject reginfo = BurlUtil.buildJson("email",email,"password",rpwd1,"salt",salt);
   JSONObject post = cli_main.createHttpPost("register",reginfo);
   
   if (cli_main.checkResponse(post,"registration")) {
      
      if (save) cli_main.saveUserPass(email,pwd); 
      boolean login = tryLogin(email,pwd,null);
      if (login) {
         IvyLog.logI("BURLCLI","Registration successful -- logged in");
       }
      else {
         IvyLog.logI("BURLCLI","Registration successful -- log in when validated");
       }
    }
}


private void badRegisterArgs()
{
   IvyLog.logI("BURLCLI","register <email> [<password>] [-save]");
}



/********************************************************************************/
/*                                                                              */
/*      Login Command                                                           */
/*                                                                              */
/********************************************************************************/

void handleLogin(List<String> args)
{
   String email = null;
   String pwd = null;
   boolean save = false;
   
   for (String s : args) {
      if (s.startsWith("-")) {
         if (s.startsWith("-s")) {                      // -save
            save = true;
          }
         else {
            badLoginArgs();
            return;
          }
       }
      else if (email == null) email = s;
      else if (pwd == null) pwd = s;
      else {
         badLoginArgs();
         return;
       }
    }
   if (email == null) {
      badLoginArgs();
      return;
    }
   
   if (pwd == null) {
      pwd = readPassword();
      if (pwd == null) return;
    }
   
   boolean sts = tryLogin(email,pwd,"login");
   if (sts) {
      IvyLog.logI("BURLCLI","Login successful");
      if (save) cli_main.saveUserPass(email,pwd);
      cli_main.autoSetLibrary(); 
    }
   else {
      IvyLog.logI("BURLCLI","Login failed");
    }
}



boolean tryLogin(String email,String pwd,String cmd)
{
   if (email == null) return false;
   
   if (pwd == null) {
      pwd = readPassword();
    }
   if (pwd == null) return false;
   
   JSONObject pre = cli_main.createHttpGet("login","email",email);
   if (!cli_main.checkResponse(pre,cmd)) return false;
   
   String salt = pre.getString("salt");
   String code = pre.getString("code");
   
   String rpwd0 = BurlUtil.secureHash(pwd);
   String rpwd1 = BurlUtil.secureHash(rpwd0 + salt);
   String rpwd2 = BurlUtil.secureHash(rpwd1 + code);
   
   JSONObject login = BurlUtil.buildJson("email",email,"password",rpwd2);
   
   JSONObject post = cli_main.createHttpPost("login",login);
   
   if (cli_main.checkResponse(post,cmd)) {
      cli_main.setLoggedIn(true); 
      return true;
    }
   
   return false;
}



private void badLoginArgs()
{
   IvyLog.logI("BURLCLI","login <email> [<password>] [-save]");
}




/********************************************************************************/
/*                                                                              */
/*      Logout command                                                          */
/*                                                                              */
/********************************************************************************/

void handleLogout(List<String> args) 
{
   if (!cli_main.isLoggedIn()) return;
   
   if (args.size() > 0) {
      badLogoutArgs();
      return;
    }
   
   JSONObject rslt = cli_main.createHttpGet("logout");
   if (cli_main.checkResponse(rslt,"logout")) {
      IvyLog.logI("BURLCLI","Logout successful");
      cli_main.setLoggedIn(false);
    }
}



private void badLogoutArgs()
{
   IvyLog.logI("BURLCLI","logout");
}




/********************************************************************************/
/*                                                                              */
/*      Remove user                                                             */
/*                                                                              */
/********************************************************************************/

void handleRemoveUser(List<String> args)
{
   if (!cli_main.isLoggedIn()) return;
   
   if (args.size() > 0) {
      badRemoveUserArgs();
      return;
    }
   
   JSONObject rslt = cli_main.createHttpPost("logout",null);
   if (cli_main.checkResponse(rslt,"removeuser")) {
      IvyLog.logI("BURLCLI","User removed; logged out");
      cli_main.setLoggedIn(false);
    }
}



private void badRemoveUserArgs()
{
   IvyLog.logI("BURLCLI","removeuser");
}




/********************************************************************************/
/*                                                                              */
/*      Change password command                                                 */
/*                                                                              */
/********************************************************************************/

void handleChangePassword(List<String> args)
{
   if (!cli_main.isLoggedIn()) return;
   
   String pwd = null;
   for (String s : args) {
      if (s.startsWith("-")) {
            badPasswordArgs();
            return;
       }
      else if (pwd == null) pwd = s;
      else {
         badPasswordArgs();
         return;
       }
    }
   if (pwd == null) {
      pwd = readPassword();
      if (pwd == null) return;
    }
   
   JSONObject cargs = BurlUtil.buildJson("password",pwd);
   JSONObject rslt = cli_main.createHttpPost("changepassword",cargs);
   if (cli_main.checkResponse(rslt,"password update")) {
      IvyLog.logI("BURLCLI","Password updated");
      cli_main.updatePassword(pwd); 
    }
}



private void badPasswordArgs()
{
   IvyLog.logI("BURLCLI","password [new password]");
}


/********************************************************************************/
/*                                                                              */
/*      Forgot password command                                                 */
/*                                                                              */
/********************************************************************************/

void handleForgotPassword(List<String> args)
{
   String email = null;
   for (String s : args) {
      if (s.startsWith("-")) {
         badForgotArgs();
         return;
       }
      else if (email == null) email = s;
      else {
         badForgotArgs();
         return;
       }
    }
   if (email == null) {
      badForgotArgs();
    }
   
   JSONObject cargs = BurlUtil.buildJson("email",email);
   JSONObject rslt = cli_main.createHttpPost("forgotpassword",cargs);
   if (cli_main.checkResponse(rslt,"forgotten password reset")) {
      IvyLog.logI("BURLCLI","Password reset email sent");
    }
}



private void badForgotArgs()
{
   IvyLog.logI("BURLCLI","forgot email");
}


/********************************************************************************/
/*                                                                              */
/*      User validation commanmd                                                */
/*                                                                              */
/********************************************************************************/

void handleUserValidation(List<String> args)
{
   String email = null;
   String code = null;
   
   for (String s : args) {
      if (s.startsWith("-")) {
         badValidateArgs();
       }
      else if (email == null) email = s;
      else if (code == null) code = s;
      else {
         badValidateArgs();
         return;
       }
    }
   if (email == null || code == null) {
      badValidateArgs();
      return;
    }
   
   JSONObject rslt = cli_main.createHttpGet("forgotpassword",
         "email",email,"code",code);
   if (cli_main.checkResponse(rslt,"validation")) {
      IvyLog.logI("BURLCLI","User validated");
    }
}



private void badValidateArgs()
{
   IvyLog.logI("BURLCLI","validate <email> <code>");
}



/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

private String readPassword()
{
   Console c = System.console();
   if (c == null) {
      IvyLog.logE("BURLCLI","No console -- can't read password");
      System.exit(1);
    }
   char [] pwdarr = c.readPassword("Enter password: ");
   if (pwdarr != null) {
      String pwd = new String(pwdarr);
      Arrays.fill(pwdarr,' ');
      return pwd;
    }
   
   // might want user to reenter and check they match
   
   return null;
}



}       // end of class CliUserCommands




/* end of CliUserCommands.java */

