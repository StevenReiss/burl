/********************************************************************************/
/*                                                                              */
/*              ControlWorkThread.java                                          */
/*                                                                              */
/*      Worker thread for adding items                                          */
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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.brown.cs.burl.burl.BurlUser;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.burl.burl.BurlWorkItem;
import edu.brown.cs.ivy.file.IvyLog;

class ControlWorkThread extends Thread implements ControlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ControlStorage burl_store;
private BlockingQueue<BurlWorkItem> work_queue;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlWorkThread(ControlMain cm)
{
   super("ISBN/LCCN Adder Thread");
   
   burl_store = cm.getStorage();
   work_queue = new LinkedBlockingQueue<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void addTask(Number lid,Number uid,List<String> isbns,BurlUpdateMode mode,boolean count)
{
   for (String isbn : isbns) {
      burl_store.addToWorkQueue(lid,uid,isbn,mode,count);
    }
   synchronized (this) {
      notifyAll();
    }
}


/********************************************************************************/
/*                                                                              */
/*      Run methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public void run() {
   for ( ; ; ) {
      String id = null;
      try {
         BurlWorkItem wi = getNextItem();
         id = wi.getItem();
         ControlLibrary lib = burl_store.findLibraryById(wi.getLibraryId());
         try {
            if (id.startsWith("@")) {
               BurlUser user = burl_store.findUserById(wi.getUserId());
               String subj = "BURL upload request status";
               String body = "BURL upload requested email:\n\t" + id.substring(1).trim() +
                     "\n for library " + lib.getName();
               BurlUtil.sendEmail(user.getEmail(),subj,body);
             }
            else if (lib != null) {
               String err = lib.addToLibrary(wi.getItem(),wi.getUpdateMode()); 
               if (err != null) {
                  BurlUser user = burl_store.findUserById(wi.getUserId());
                  if (user != null) {
                     String subj = "Status of BURL upload request";
                     String body = "Problem with upload.  " + err;
                     BurlUtil.sendEmail(user.getEmail(),subj,body);
                   }
                }
             }
          }
         finally {
            burl_store.removeFromWorkQueue(wi.getItemId());
          }
//       BurlUser user = burl_store.findUserById(wi.getUserId());
//       if (user != null) {
//          if (!workPending(wi)) {
//             String subj = "Finished BURL upload request";
//             String body = "All the ISBNs/LCCNs you requested have been processed " +
//                   "ending with " + wi.getItem() + ".";
//             BurlUtil.sendEmail(user.getEmail(),subj,body);
//           }
//        }
       }
      catch (InterruptedException e) { }
      catch (Throwable t) {
         IvyLog.logE("BURL","Problem working on item " + id,t);
       }
    }
}


private BurlWorkItem getNextItem() throws InterruptedException
{
   for ( ; ; ) {
      BurlWorkItem wi = work_queue.poll();
      if (wi != null) return wi;
      loadItems(true);                      // waits for next item
    }
}



// private boolean workPending(BurlWorkItem itm)
// {
// for (BurlWorkItem wi : work_queue) {
//    if (wi.getUserId() == itm.getUserId() &&
//          wi.getLibraryId() == itm.getLibraryId()) return true;
//  }
// int ct = burl_store.getPendingCount(itm.getLibraryId(),itm.getUserId());
// if (ct > 0) return true;
// 
// return false;
// }
// 

/********************************************************************************/
/*                                                                              */
/*      Load items from the database                                            */
/*                                                                              */
/********************************************************************************/

private void loadItems(boolean wait)
{
   List<BurlWorkItem> items;
   for ( ; ; ) {
      synchronized (this) {
         items = burl_store.getWorkQueue();
         if (items != null && !items.isEmpty()) break;
         if (!wait) break;
         try {
            wait();
          }
         catch (InterruptedException e) { }
       }
    }
   if (items != null) { 
      work_queue.addAll(items);
    }
}


}       // end of class ControlWorkThread




/* end of ControlWorkThread.java */

