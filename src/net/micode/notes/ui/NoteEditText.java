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
import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.EditText;

import net.micode.notes.R;

import java.util.HashMap;
import java.util.Map;

/**
 * 便签编辑文本框
 * 
 * 功能职责：
 * 1. 提供便签内容编辑的自定义EditText控件
 * 2. 支持电话号码和URL的自动识别和点击
 * 3. 处理文本选择和右键菜单功能
 * 4. 监听文本内容变化，通知外部更新
 * 5. 支持特殊按键处理(如删除键)
 * 
 * 与软件功能的对应关系：
 * - 便签编辑：提供富文本编辑能力
 * - 联系人关联：识别电话号码，支持点击拨号
 * 
 * 特性：
 * - 继承EditText，扩展点击链接功能
 * - 使用URLSpan实现电话号码和URL的可点击
 * - 通过OnTextViewChangeListener回调通知文本变化
 */
public class NoteEditText extends EditText {
    private static final String TAG = "NoteEditText";
    private int mIndex;
    private int mSelectionStartBeforeDelete;

    private static final String SCHEME_TEL = "tel:" ;
    private static final String SCHEME_HTTP = "http:" ;
    private static final String SCHEME_EMAIL = "mailto:" ;

    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel);
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web);
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email);
    }

    /**
     * Call by the {@link NoteEditActivity} to delete or add edit text
     */
    public interface OnTextViewChangeListener {
        /**
         * Delete current edit text when {@link KeyEvent#KEYCODE_DEL} happens
         * and the text is null
         */
        void onEditTextDelete(int index, String text);

        /**
         * Add edit text after current edit text when {@link KeyEvent#KEYCODE_ENTER}
         * happen
         */
        void onEditTextEnter(int index, String text);

        /**
         * Hide or show item option when text change
         */
        void onTextChange(int index, boolean hasText);
    }

    private OnTextViewChangeListener mOnTextViewChangeListener;

    public NoteEditText(Context context) {
        super(context, null);
        mIndex = 0;
    }

    public void setIndex(int index) {
        mIndex = index;
    }

    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener;
    }

    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle);
    }

    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    /**
     * 处理触摸事件 - 实现精确的光标定位
     * 
     * @param event 触摸事件对象
     * @return 是否消费了该事件
     * 
     * 功能说明：
     * 将触摸坐标转换为文本光标位置，实现点击文本的精确定位
     * 
     * 技术细节：
     * 1. 获取触摸的屏幕坐标(x, y)
     * 2. 减去padding和滚动偏移，得到文本区域内的相对坐标
     * 3. 使用Layout计算坐标对应的文本行号和字符偏移
     * 4. 设置Selection到计算出的位置
     * 
     * 【改进建议-富文本功能】
     * 可在此处添加格式化文本的点击检测：
     * - 点击链接时打开浏览器
     * - 点击待办项时切换勾选状态
     * - 双击文字时弹出格式工具栏
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // ========== 块1：坐标转换 - 屏幕坐标到文本坐标 ==========
                // 【语句级】获取触摸点的原始屏幕坐标
                int x = (int) event.getX();
                int y = (int) event.getY();
                
                // 【语句级】减去左侧padding，得到内容区域的x坐标
                x -= getTotalPaddingLeft();
                // 【语句级】减去顶部padding，得到内容区域的y坐标
                y -= getTotalPaddingTop();
                
                // 【语句级】加上水平滚动偏移（处理文本过长横向滚动的情况）
                x += getScrollX();
                // 【语句级】加上垂直滚动偏移（处理文本过长纵向滚动的情况）
                y += getScrollY();

                // ========== 块2：文本位置计算 ==========
                // 【语句级】获取文本的Layout对象，用于坐标和文本位置的转换
                Layout layout = getLayout();
                // 【语句级】根据y坐标获取对应的文本行号
                int line = layout.getLineForVertical(y);
                // 【语句级】根据行号和x坐标获取字符在文本中的偏移量
                int off = layout.getOffsetForHorizontal(line, x);
                
                // ========== 块3：设置光标位置 ==========
                // 【语句级】将Selection设置到计算出的偏移位置
                // Selection.setSelection会触发光标移动和可能的文本选择
                Selection.setSelection(getText(), off);
                break;
        }

        // 调用父类方法处理其他触摸事件（如滚动、长按等）
        return super.onTouchEvent(event);
    }

    /**
     * 按键按下事件处理
     * 
     * @param keyCode 按键代码
     * @param event 按键事件对象
     * @return true表示事件已处理，false表示交给父类处理
     * 
     * 功能说明：
     * 处理特殊按键（回车键、删除键）的按下事件
     * 
     * 关键处理：
     * - KEYCODE_ENTER: 检查是否有监听器，决定是否拦截
     * - KEYCODE_DEL: 记录删除前的光标位置（用于后续判断是否需要合并文本框）
     * 
     * 【改进建议-富文本功能】
     * 可在此处添加Markdown快捷键支持：
     * - Ctrl+B: 加粗选中文本
     * - Ctrl+I: 斜体选中文本
     * - Tab: 增加缩进层级
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                // 如果设置了监听器，返回false让监听器处理回车
                // 监听器会创建新的EditText实现多行分段编辑
                if (mOnTextViewChangeListener != null) {
                    return false;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                // 【语句级】记录删除键按下时的光标位置
                // 这个位置将在onKeyUp中用于判断是否应该合并编辑框
                mSelectionStartBeforeDelete = getSelectionStart();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 按键抬起事件处理 - 处理编辑框的分割和合并
     * 
     * @param keyCode 按键代码
     * @param event 按键事件对象
     * @return true表示事件已处理
     * 
     * 功能说明：
     * 实现多EditText编辑模式的核心逻辑
     * 
     * 四层级注释示例：
     * 【方法级】此方法处理回车分割和删除合并两个关键交互
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode) {
            // ========== Case 1：处理删除键 - 可能触发EditText合并 ==========
            case KeyEvent.KEYCODE_DEL:
                if (mOnTextViewChangeListener != null) {
                    /**
                     * 【块级】判断是否需要合并EditText的条件：
                     * 1. 删除前光标在位置0（文本开头）
                     * 2. 当前不是第一个EditText（mIndex != 0）
                     * 
                     * 满足条件时，通知监听器删除当前EditText，
                     * 并将内容合并到上一个EditText
                     */
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) {
                        // 【语句级】调用监听器的删除回调，传递当前索引和文本内容
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString());
                        return true;
                    }
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
                
            // ========== Case 2：处理回车键 - 分割EditText ==========
            case KeyEvent.KEYCODE_ENTER:
                if (mOnTextViewChangeListener != null) {
                    /**
                     * 【块级】分割EditText的逻辑：
                     * 1. 获取光标位置
                     * 2. 将光标后的文本提取出来
                     * 3. 保留光标前的文本在当前EditText
                     * 4. 将光标后的文本插入到新的EditText
                     */
                    // 【语句级】获取当前光标位置
                    int selectionStart = getSelectionStart();
                    // 【语句级】提取光标位置到文本末尾的子串
                    String text = getText().subSequence(selectionStart, length()).toString();
                    // 【语句级】保留文本开头到光标位置的内容
                    setText(getText().subSequence(0, selectionStart));
                    // 【语句级】通知监听器在下一个位置插入EditText，内容为提取的后半段
                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text);
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 焦点改变事件处理
     * 
     * @param focused 是否获得焦点
     * @param direction 焦点移动方向
     * @param previouslyFocusedRect 之前焦点的位置
     * 
     * 功能说明：
     * 监控焦点变化，通知监听器更新UI状态
     * 
     * 应用场景：
     * - EditText失去焦点且为空时，可能需要隐藏或删除该EditText
     * - EditText获得焦点时，显示编辑工具栏
     * 
     * 【改进建议-富文本功能】
     * 可在获得焦点时显示格式工具栏，失去焦点时隐藏
     */
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mOnTextViewChangeListener != null) {
            if (!focused && TextUtils.isEmpty(getText())) {
                // 【语句级】失去焦点且文本为空，通知监听器（可能删除该EditText）
                mOnTextViewChangeListener.onTextChange(mIndex, false);
            } else {
                // 【语句级】有焦点或有内容，保持EditText显示
                mOnTextViewChangeListener.onTextChange(mIndex, true);
            }
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        if (getText() instanceof Spanned) {
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class);
            if (urls.length == 1) {
                int defaultResId = 0;
                for(String schema: sSchemaActionResMap.keySet()) {
                    if(urls[0].getURL().indexOf(schema) >= 0) {
                        defaultResId = sSchemaActionResMap.get(schema);
                        break;
                    }
                }

                if (defaultResId == 0) {
                    defaultResId = R.string.note_link_other;
                }

                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                        new OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                // goto a new intent
                                urls[0].onClick(NoteEditText.this);
                                return true;
                            }
                        });
            }
        }
        super.onCreateContextMenu(menu);
    }
}
