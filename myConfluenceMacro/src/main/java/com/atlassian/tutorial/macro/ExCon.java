package com.atlassian.tutorial.macro;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;

import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.folder.CalendarFolder;
import microsoft.exchange.webservices.data.core.service.item.Appointment;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.search.CalendarView;
import microsoft.exchange.webservices.data.search.FindItemsResults;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.sql.*;
import java.util.Date;

import static java.awt.Color.pink;


public class ExCon implements Macro {

    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) throws MacroExecutionException {

        String fromOutlook = "";
        String username = map.get("Username");
        String password = map.get("Password");
        String calendarID = map.get("CalendarID");


        // Specifies Exchange version, (any newer works as well)
        ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        // Log in with the respective Exchange account
        ExchangeCredentials credentials = new WebCredentials(username, password);
        // Verifies the credentials
        service.setCredentials(credentials);

        // Finds the URI for the E-mail
        try {
            service.autodiscoverUrl(username, new RedirectionUrlCallback());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sets the Date Format
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Sets start Date
        Date startDate = null;
        try {
            startDate = formatter.parse("2017-04-10 12:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Sets end Date
        Date endDate = null;
        try {
            endDate = formatter.parse("2017-05-30 13:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Defines which Calendar Folder to use
        CalendarFolder cf = null;
        try {
            cf = CalendarFolder.bind(service, WellKnownFolderName.Calendar);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Makes an array of Calendar Results
        FindItemsResults<Appointment> findResults = null;
        try {
            findResults = cf.findAppointments(new CalendarView(startDate, endDate));
        } catch (Exception e) {
            e.printStackTrace();
        }

        findResults.getItems();

        LinkedList<Event> eventsList = new LinkedList<Event>();
        eventParameters ep = new eventParameters();
        Connection myConn;
        eventInserter ei = new eventInserter();
        String vacationID= null;
        try {
            ep.setUser("tcomkproj2017");
            ep.setPassword("tcomkproj2017");
            ep.setdbUrl("localhost:3306/confluence");
            myConn = DriverManager.getConnection(ep.getDbUrl(), ep.getUser(), ep.getPassword());
            for (Appointment appt : findResults.getItems()) {


                // Make a new Event object to hold data of one appointment
                // Loads appt
                try {
                    appt.load();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ep.setAll_day("0");                //all day 1
                try {
                    ep.setCreated(ConvertTime(appt.getDateTimeCreated(), true));   //created
                } catch (ParseException x) {
                    x.printStackTrace();
                }
                ep.setDescription("");                //description
                try {
                    ep.setEnd(ConvertTime(appt.getEnd(), true));   //End
                } catch (ParseException x) {
                    x.printStackTrace();
                }
                try {
                    ep.setLast_modified(ConvertTime(appt.getLastModifiedTime(), true));   //Last_Modified
                } catch (ParseException x) {
                    x.printStackTrace();
                }
                ep.setLocation("");      //Location
                ep.setOrganiser("4028b8815babae10015babb056780000");//Organiser
                ep.setRecurrence_id_timestamp(0);            //rec. Id Timestamp
                ep.setRecurrence_rule("");            //Rec. Rule
                ep.setReminder_setting_id("");           //Reminder_SETTING_ID
                ep.setSequence("0");              //SEQUENCE
                try {
                    ep.setStart(ConvertTime(appt.getStart(), true));  //START
                } catch (ParseException x) {
                    x.printStackTrace();
                }
                //SUB_CALENDAR_ID
                //this is for different vacation event type

                if (appt.getCategories().toString().equals("Orange category,")) // for Vacation events
                {

                    vacationID = SubCalendarID(calendarID, myConn , "Orange");}

                else {
                    vacationID = SubCalendarID(calendarID, myConn , "Blue");}

                try { ep.setSub_calendar_id(vacationID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    ep.setSummary(appt.getSubject().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //ep.setUrl("NULL");           //URL
                ep.setUrl(appt.getMeetingWorkspaceUrl());
                try {
                    ep.setUtc_end(ConvertTime(appt.getEnd(), false));  //UTC_END
                    //ep.setUtc_start(ConvertTime(appt.getStart(), false));  //UTC_START
                } catch (ParseException x) {
                    x.printStackTrace();
                }
                try {
                    ep.setUtc_start(ConvertTime(appt.getStart(), false));  //UTC_START
                } catch (ParseException x) {
                    x.printStackTrace();
                }
                // Get current time
                SimpleDateFormat test = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z--'");
                test.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = appt.getICalDateTimeStamp();
                //Create a random unique value for each event that is in the calendar of Outlook
                double random = Math.random() * 1000000000;
                ep.setVevent_uid(test.format(date) + String.valueOf(random) + "@130.229.188.219");//VEVENT UID
                ei.insert(ep, myConn);
            }





            myConn.close();
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
        return  vacationID;
    }


    // Simple error checker for the URI
    static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
        public boolean autodiscoverRedirectionUrlValidationCallback(
                String redirectionUrl) {
            return redirectionUrl.toLowerCase().startsWith("https://");
        }

    }

    /* Converts the time acquired from the specific Outlook event to the compatible Unix Epoch time format.
       time -> time & date of the event
       localtime -> Determines whether or not the time is to be local or UTC
    */
    private static String ConvertTime(Date time, boolean localtime) throws Exception {

        Date date = null;
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

        if (localtime) {
            try {
                date = df.parse(time.toString());
            } catch (ParseException exc) {
                exc.printStackTrace();
            }
            return String.valueOf(date.getTime());
        } else {
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                date = df.parse(df.format(time));
            } catch (ParseException exc) {
                exc.printStackTrace();
            }
            return String.valueOf(date.getTime());
        }
    }
    /* It return the SubcalendarID of given  ParentID*/
    private static String SubCalendarID(String parentID , Connection myConn ,String color ){

        String resultID= " ";
        ResultSet myRs ;

        try {
            //Create a statement
            Statement myStm = myConn.createStatement();
            // Get the child-ID of the parentID to crrosponding color
            if ( color.equals("Orange")) {
                myRs = myStm.executeQuery("SELECT ID FROM confluence.ao_950dc3_tc_subcals WHERE PARENT_ID= '" + parentID + "' AND COLOUR='subcalendar-orange';");
                while (myRs.next()) {

                    resultID = myRs.getString("ID");
                    return   resultID;
                }
            }
            else
            { myRs = myStm.executeQuery( "SELECT ID FROM confluence.ao_950dc3_tc_subcals WHERE PARENT_ID= '"+parentID+"' AND COLOUR='subcalendar-blue';");
                while (myRs.next()) {

                    resultID =myRs.getString("ID");
                    return   resultID;
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }


        return   resultID;


    }
    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }
}
