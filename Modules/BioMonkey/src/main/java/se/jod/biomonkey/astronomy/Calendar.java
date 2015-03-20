/*
 * Copyright (c) 2012, Andreas Olofsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package se.jod.biomonkey.astronomy;

/**
 * A basic calendar. Used to set sun direction and other things.
 * 
 * @author Andreas
 */
public final class Calendar {
    protected static float inv60 = 1/60f;
    
    protected byte[] DAYS = {31,28,31,30,31,30,31,31,30,31,30,31};
    
    protected double minute;
    protected int hour;
    protected int day;
    protected int month;
    protected int year;
    
    protected int dayInYear;
    
    //Julian day number.
    protected long JDN;
    protected float tMult = 1f;
    
    protected String dateString = "";
    
    public Calendar(int year, int month, int day, int hour, int minute, float tMult){
        reset(year,month,day,hour,minute,tMult);
    }
    
    public void reset(int year, int month, int day, int hour, int minute, float tMult){
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.tMult = tMult;
        refresh();
    }
    
    public void refresh(){
        this.JDN = getJulianDayFromGregorian(year,month,day);
        updateDaysInYear();
        updateDateString();
    }
    
    public void update(float tpf){
        minute += tpf*inv60*tMult;
        if(minute >= 60){
            minute = 0.0;
            updateHour();
        }
    }
    
    public void updateDaysInYear(){
        if (year % 4 == 0){
            DAYS[1] = 29;
        }
        dayInYear = 0;
        for(int i = 0; i < month - 1; i++){
            dayInYear += DAYS[i];
        }
        dayInYear += day;
    }
    
    public int getDayInYear(){
        return dayInYear;
    }
    
    protected void updateHour(){
        
        int oldDay = day;
        
        if(hour != 23){
            hour++;
        } else {
            hour = 0;
            if(month == 12 && day == 31){
                year++;
                month = 1;
                day = 1;
                updateDaysInYear();
            }
            else if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12){
                if(day == 31){
                    month++;
                    day = 1;
                } else {
                    day++;
                    hour = 0;
                } if(month == 4 || month == 6 || month == 9 || month == 11){
                    if(day == 30){
                        month++;
                        day = 1;
                    } else {
                        day++;
                    }
                } if(month == 2){
                    if(day == 28 && year % 4 != 0){
                        month++;
                        day = 1;
                    } else {
                        day++;
                    }
                    if(day == 29 && year %4 == 0){
                        month++;
                        day = 1;
                    }
                } 
            }
            //Increment julian day and day in year.
            if(oldDay != day){
                dayInYear += 1;
                JDN += 1;
            }
            updateDateString();
        }
    }
    
    public final long getJulianDayFromGregorian(int year,int month, int day){
        
        int a = (14 - month)/12;
        int m = month + 12*a - 3;
        int y = year + 4800 - a;
        
        return (long)(day + (153*m + 2)/2 + 365*y + y/4 - y/100 + y/400 - 32045);
    }
    
    public void setTimeMult(float tMult){
        this.tMult = tMult;
    }
    
    //Returns date "yyyy/mm/dd"
    public String getDateString(){
        return dateString;
    }
    
    public void updateDateString(){
        // TODO use a proper formatter.
        StringBuilder s = new StringBuilder("Date: ");
        s.append(year).append("/");
        if(month < 10){
            s.append("0");
        }
        s.append(month);
        s.append("/");
        if(day < 10){
            s.append("0");
        }
        s.append(day);
        
        dateString = s.toString();
    }
    
    //Returns time "hh:mm"
    public String getTimeString(){
        StringBuilder s = new StringBuilder("Time: ");
        if(hour < 10){
            s.append("0");
        }
        s.append(hour).append(":");
        if(minute < 10){
            s.append("0");
        }
        s.append((int)(minute));
        
        return s.toString();
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
        refresh();
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
        refresh();
    }

    public double getMinute() {
        return minute;
    }

    public void setMinute(double minute) {
        this.minute = minute;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
        refresh();
    }

    public float gettMult() {
        return tMult;
    }

    /**
     * Set the time multiplier. Passed time is calculated as
     * 'time * tMult'. A tMult value of 10 means every real-time
     * second is equal to 10 app-time seconds.
     * @param tMult 
     */
    public void setTMult(float tMult) {
        this.tMult = tMult;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
        refresh();
    }

    /**
     * Get the Julian day number.
     * @return 
     */
    public long getJDN() {
        return JDN;
    }
    
    
}
