/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;

/**
 * 系统启动后的闹钟初始化接收器
 * 
 * 功能职责：
 * 1. 监听系统启动完成(BOOT_COMPLETED)广播
 * 2. 系统重启后重新注册所有便签的闹钟提醒
 * 3. 从数据库查询所有设置了提醒的便签，逐一恢复闹钟
 * 
 * 与软件功能的对应关系：
 * - 提醒功能：确保系统重启后提醒功能不丢失
 * 
 * 工作原理：
 * Android系统重启后，所有AlarmManager设置的闹钟都会被清除。
 * 该接收器在系统启动完成后，读取数据库中所有有提醒时间的便签，
 * 重新通过AlarmManager注册闹钟，保证提醒功能的持续性。
 */
public class AlarmInitReceiver extends BroadcastReceiver {

    private static final String [] PROJECTION = new String [] {
        NoteColumns.ID,
        NoteColumns.ALERTED_DATE
    };

    private static final int COLUMN_ID                = 0;
    private static final int COLUMN_ALERTED_DATE      = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        long currentDate = System.currentTimeMillis();
        Cursor c = context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[] { String.valueOf(currentDate) },
                null);

        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    long alertDate = c.getLong(COLUMN_ALERTED_DATE);
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, 0);
                    AlarmManager alermManager = (AlarmManager) context
                            .getSystemService(Context.ALARM_SERVICE);
                    alermManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                } while (c.moveToNext());
            }
            c.close();
        }
    }
}
