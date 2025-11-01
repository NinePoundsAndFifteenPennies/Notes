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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 闹钟广播接收器
 * 
 * 功能职责：
 * 1. 接收系统AlarmManager发送的闹钟广播
 * 2. 启动AlarmAlertActivity显示提醒界面
 * 
 * 与软件功能的对应关系：
 * - 提醒功能：作为闹钟机制的接收端，连接系统闹钟和提醒界面
 * 
 * 工作流程：
 * 用户设置提醒 -> NoteEditActivity通过AlarmManager注册闹钟 -> 
 * 时间到达 -> 系统发送广播 -> AlarmReceiver接收 -> 启动AlarmAlertActivity
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, AlarmAlertActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
