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

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import net.micode.notes.data.Notes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * 便签列表适配器
 * 
 * 功能职责：
 * 1. 管理便签列表的数据展示，连接数据源和ListView
 * 2. 为每个便签项创建和复用视图(ViewHolder模式)
 * 3. 处理便签的选择状态，支持多选模式
 * 4. 根据便签类型(普通便签、通话记录便签)显示不同的视图样式
 * 5. 管理小部件绑定的便签，跟踪widget关联信息
 * 
 * 与软件功能的对应关系：
 * - 便签管理：在列表界面展示便签数据
 * - 批量操作：支持多选模式，可批量选择便签进行操作
 * - 桌面小部件：记录并标识哪些便签绑定了widget
 * 
 * 设计模式：
 * - Adapter模式：继承CursorAdapter，适配Cursor数据到ListView
 * - ViewHolder模式：复用列表项视图，提高滚动性能
 */
public class NotesListAdapter extends CursorAdapter {
    private static final String TAG = "NotesListAdapter";
    private Context mContext;
    private HashMap<Integer, Boolean> mSelectedIndex;
    private int mNotesCount;
    private boolean mChoiceMode;

    public static class AppWidgetAttribute {
        public int widgetId;
        public int widgetType;
    };

    /**
     * 构造函数 - 初始化便签列表适配器
     * 
     * @param context 上下文对象
     * 
     * 功能说明：
     * 1. 调用父类CursorAdapter构造函数
     * 2. 初始化选择状态HashMap
     * 3. 初始化便签计数器
     * 
     * 【改进建议-标签功能】
     * 未来可在此处初始化标签数据缓存，提高标签显示性能
     */
    public NotesListAdapter(Context context) {
        // 调用父类构造，初始cursor为null（后续通过changeCursor设置）
        super(context, null);
        // 初始化选择状态映射表：position -> 是否选中
        mSelectedIndex = new HashMap<Integer, Boolean>();
        // 保存context引用，用于后续操作
        mContext = context;
        // 初始化便签数量为0
        mNotesCount = 0;
    }

    /**
     * 创建新的列表项视图（Adapter模式的核心方法）
     * 
     * @param context 上下文
     * @param cursor 数据游标，指向当前便签数据
     * @param parent 父视图容器
     * @return 创建的NotesListItem视图对象
     * 
     * 功能说明：
     * 当ListView需要显示新项时调用此方法创建视图
     * 使用ViewHolder模式，视图会被复用以提高性能
     * 
     * 【改进建议-标签功能】
     * 可返回增强的NotesListItem，内置标签显示区域
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // 创建并返回NotesListItem实例
        // NotesListItem内部实现了ViewHolder模式
        return new NotesListItem(context);
    }

    /**
     * 绑定数据到已存在的视图（Adapter核心方法）
     * 
     * @param view 要绑定数据的视图（由newView创建或复用）
     * @param context 上下文
     * @param cursor 数据游标，包含便签的所有字段数据
     * 
     * 功能说明：
     * 将cursor中的便签数据绑定到NotesListItem视图上
     * 这是ListView滚动时频繁调用的方法，需要高效执行
     * 
     * 四层级注释示例：
     * 【方法级】此方法负责数据和视图的绑定，是列表显示的核心
     * 【块级】分为两个主要代码块：类型检查和数据绑定
     * 【语句级】每条语句都有其特定作用
     * 【注释】详细解释关键决策点
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // ========== 块1：类型检查和数据准备 ==========
        // 【语句级】确保视图是NotesListItem类型，避免类型转换异常
        if (view instanceof NotesListItem) {
            // 【语句级】从cursor创建NoteItemData对象
            // NoteItemData封装了便签的所有显示数据：标题、时间、类型等
            NoteItemData itemData = new NoteItemData(context, cursor);
            
            // ========== 块2：绑定数据到视图 ==========
            // 【语句级】调用NotesListItem.bind()方法绑定数据
            // 参数说明：
            // - context: 用于访问资源
            // - itemData: 封装的便签数据
            // - mChoiceMode: 是否处于多选模式
            // - isSelectedItem: 当前项是否被选中
            ((NotesListItem) view).bind(context, itemData, mChoiceMode,
                    isSelectedItem(cursor.getPosition()));
                    
            // 【改进建议-标签功能】
            // 可在此处添加标签数据的绑定逻辑：
            // List<Tag> tags = getTagsForNote(itemData.getId());
            // ((NotesListItem) view).bindTags(tags);
        }
    }

    /**
     * 设置指定位置的列表项选中状态
     * 
     * @param position 列表项位置
     * @param checked 是否选中
     * 
     * 功能说明：
     * 1. 更新选中状态到HashMap
     * 2. 通知Adapter刷新视图
     * 
     * 应用场景：
     * - 用户点击列表项进入多选模式
     * - 批量操作时选择多个便签
     */
    public void setCheckedItem(final int position, final boolean checked) {
        // 将position和checked状态存入HashMap
        mSelectedIndex.put(position, checked);
        // 触发Adapter重新绑定数据，更新UI显示
        notifyDataSetChanged();
    }

    /**
     * 判断当前是否处于选择模式
     * 
     * @return true表示处于多选模式，false表示普通浏览模式
     * 
     * 用途：
     * - UI根据此状态显示或隐藏复选框
     * - 控制列表项的点击行为
     */
    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    /**
     * 设置选择模式的开关
     * 
     * @param mode true开启多选模式，false关闭
     * 
     * 功能说明：
     * 1. 清空所有选中状态
     * 2. 更新模式标志
     * 
     * 触发时机：
     * - 用户长按列表项进入多选
     * - 完成批量操作退出多选
     */
    public void setChoiceMode(boolean mode) {
        // 【语句级】清空选中项集合，确保状态干净
        mSelectedIndex.clear();
        // 【语句级】更新多选模式标志
        mChoiceMode = mode;
    }

    /**
     * 全选或全不选所有便签
     * 
     * @param checked true表示全选，false表示全不选
     * 
     * 功能说明：
     * 遍历cursor中的所有便签，设置其选中状态
     * 只处理TYPE_NOTE类型，忽略文件夹等其他类型
     * 
     * 四层级注释示例：
     */
    public void selectAll(boolean checked) {
        // 【块级】获取数据游标，准备遍历所有项
        Cursor cursor = getCursor();
        
        // 【块级】循环处理每个列表项
        for (int i = 0; i < getCount(); i++) {
            // 【语句级】移动游标到位置i
            if (cursor.moveToPosition(i)) {
                // 【语句级】检查是否为普通便签类型
                // 只有TYPE_NOTE类型才能被选中，文件夹不能选
                if (NoteItemData.getNoteType(cursor) == Notes.TYPE_NOTE) {
                    // 【语句级】设置该位置的选中状态
                    setCheckedItem(i, checked);
                }
            }
        }
    }

    /**
     * 获取所有选中项的ID集合
     * 
     * @return 包含所有选中便签ID的HashSet
     * 
     * 功能说明：
     * 遍历mSelectedIndex，提取所有选中项的ID
     * 过滤掉非法ID（如根文件夹ID）
     * 
     * 应用场景：
     * - 批量删除便签
     * - 批量移动到文件夹
     * - 批量导出
     * 
     * 【改进建议-标签功能】
     * 可添加getSelectedItemsForTagging()方法
     * 专门用于批量添加标签操作
     */
    public HashSet<Long> getSelectedItemIds() {
        // 创建结果集合
        HashSet<Long> itemSet = new HashSet<Long>();
        
        // ========== 块1：遍历选中索引 ==========
        for (Integer position : mSelectedIndex.keySet()) {
            // 【语句级】检查该位置是否真的被选中
            if (mSelectedIndex.get(position) == true) {
                // 【语句级】根据position获取便签ID
                Long id = getItemId(position);
                
                // ========== 块2：ID有效性检查 ==========
                // 【语句级】过滤掉根文件夹ID（不应该出现在选中项中）
                if (id == Notes.ID_ROOT_FOLDER) {
                    Log.d(TAG, "Wrong item id, should not happen");
                } else {
                    // 【语句级】将有效ID添加到结果集
                    itemSet.add(id);
                }
            }
        }

        return itemSet;
    }

    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        HashSet<AppWidgetAttribute> itemSet = new HashSet<AppWidgetAttribute>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                Cursor c = (Cursor) getItem(position);
                if (c != null) {
                    AppWidgetAttribute widget = new AppWidgetAttribute();
                    NoteItemData item = new NoteItemData(mContext, c);
                    widget.widgetId = item.getWidgetId();
                    widget.widgetType = item.getWidgetType();
                    itemSet.add(widget);
                    /**
                     * Don't close cursor here, only the adapter could close it
                     */
                } else {
                    Log.e(TAG, "Invalid cursor");
                    return null;
                }
            }
        }
        return itemSet;
    }

    public int getSelectedCount() {
        Collection<Boolean> values = mSelectedIndex.values();
        if (null == values) {
            return 0;
        }
        Iterator<Boolean> iter = values.iterator();
        int count = 0;
        while (iter.hasNext()) {
            if (true == iter.next()) {
                count++;
            }
        }
        return count;
    }

    public boolean isAllSelected() {
        int checkedCount = getSelectedCount();
        return (checkedCount != 0 && checkedCount == mNotesCount);
    }

    public boolean isSelectedItem(final int position) {
        if (null == mSelectedIndex.get(position)) {
            return false;
        }
        return mSelectedIndex.get(position);
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        calcNotesCount();
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        calcNotesCount();
    }

    private void calcNotesCount() {
        mNotesCount = 0;
        for (int i = 0; i < getCount(); i++) {
            Cursor c = (Cursor) getItem(i);
            if (c != null) {
                if (NoteItemData.getNoteType(c) == Notes.TYPE_NOTE) {
                    mNotesCount++;
                }
            } else {
                Log.e(TAG, "Invalid cursor");
                return;
            }
        }
    }
}
