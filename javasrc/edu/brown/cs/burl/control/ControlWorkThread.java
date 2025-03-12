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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

void addTask(Number lid,List<String> isbns,BurlUpdateMode mode,boolean count)
{
   for (String isbn : isbns) {
      burl_store.addToWorkQueue(lid,isbn,mode,count);
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
      try {
         BurlWorkItem wi = getNextItem();
         ControlLibrary lib = burl_store.findLibraryById(wi.getLibraryId());
         try {
            if (lib != null) {
               List<String> itms = Collections.singletonList(wi.getItem());
               lib.addToLibrary(itms,wi.getUpdateMode(),wi.doCount());
             }
          }
         finally {
            burl_store.removeFromWorkQueue(wi.getItemId());
          }
       }
      catch (InterruptedException e) { }
      catch (Throwable t) {
         IvyLog.logE("BURL","Problem working on item",t);
       }
    }
}


private BurlWorkItem getNextItem() throws InterruptedException
{
   for ( ; ; ) {
      BurlWorkItem wi = work_queue.poll();
      if (wi != null) return wi;
      loadItems();                      // waits for next item
    }
}



/********************************************************************************/
/*                                                                              */
/*      Load items from the database                                            */
/*                                                                              */
/********************************************************************************/

private void loadItems()
{
   List<BurlWorkItem> items;
   for ( ; ; ) {
      synchronized (this) {
         items = burl_store.getWorkQueue();
         if (items != null && !items.isEmpty()) break;
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

