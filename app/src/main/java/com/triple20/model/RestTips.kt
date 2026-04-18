package com.triple20.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * 休息提示语库
 * 来源：Stretchly
 * 支持用户自定义编辑，持久化到 SharedPreferences
 */
object RestTips {
    private const val PREFS_NAME = "rest_tips"
    private const val KEY_TIPS = "tips"

    private val defaultTips = listOf(
        "去倒杯水喝吧",
        "慢慢向左看，然后向右看",
        "慢慢向上看，然后向下看",
        "闭上眼睛，深呼吸几次",
        "闭上眼睛放松一下",
        "伸展你的双腿",
        "伸展你的手臂",
        "你的坐姿正确吗？",
        "慢慢转头，保持10秒",
        "慢慢倾斜头部，保持5-10秒",
        "站起来伸展一下",
        "注视20米外的物体",
        "想想你感激的事情",
        "为活着微笑一下吧",
        "真正符合人体工学的工位是你经常远离的工位",
        "闭上眼睛，数你的呼吸",
        "闭上眼睛，说出你听到的东西",
        "指尖放在肩膀上，向前转动肩膀10秒，然后向后转",
        "抬起右臂，向左伸展头顶，保持10秒。换另一侧重复",
        "用右手挤压左手的每根手指。换另一侧重复",
        "站起来做弓步。保持10秒，然后换另一条腿",
        "闭上眼睛，不加评判地注意当下的一切",
        "每20分钟用20秒时间注视20英尺外的物体",
        "如果需要帮助，就寻求帮助",
        "一次只做一件事",
        "你的注意力花得明智吗？",
        "改变你的坐姿",
        "让眼睛接触自然光",
        "闭上眼睛，缓慢轻柔地将眼睛向上移到天花板，然后向下移到地板",
        "闭上眼睛，缓慢轻柔地将眼睛向左移，然后向右移",
        "甩动双手放松一下",
        "依次将每根手指的指尖触碰到拇指，形成O形",
        "握拳，然后滑动手指直到它们指向天花板，就像叫某人停止一样",
        "握拳，然后将手指扇形展开，尽可能伸展",
        "坐直，手臂垂在体侧，慢慢向一个方向转动脖子画大圈",
        "站直，慢慢用同侧手将头向肩膀倾斜，直到感到拉伸",
        "站直，手臂垂在体侧，挤压肩胛骨并保持",
        "站直，手臂举到肩膀高度。双手稍微向后移，保持一秒后返回",
        "坐在椅子边缘，躯干向一侧扭转，保持10-15秒。换另一侧重复",
        "站起来，一只脚放在附近的物体上，如椅子或脚踏凳。膝盖弯曲，保持10-15秒。换另一只脚重复",
        "双脚与肩同宽站立，身体向下蹲坐，然后站起。重复几次",
        "背靠墙站立，双手贴墙，然后上下移动手臂，就像做天使雪一样",
        "面向墙站立，双手放在墙上，做几个俯卧撑",
        "坐在椅子边缘，一条腿向前伸直，保持10-15秒。换另一条腿重复",
        "面向墙站立，一只脚在后，一只脚在前，然后向墙倾斜。换另一条腿重复",
        "一只臂伸直，用另一只臂轻轻将第一只臂的肘部推向头部。保持10-15秒。换另一只臂重复",
        "向一侧倾斜头部，然后向另一侧，然后轻轻前后倾斜",
        "一只臂横过胸前，用另一只手握住肘部，轻轻拉向胸部。换另一只臂重复",
        "站起来，一只脚放在附近的物体上，如椅子或脚踏凳，弯腰触摸脚趾。换另一条腿重复",
        "站在门口，每只手放在门的一侧，然后轻轻向前倾，直到感到胸部拉伸",
        "坐在椅子边缘或地板上，脚掌并拢，用肘部轻轻按压以拉伸大腿内侧"
    )

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs!!.contains(KEY_TIPS)) {
            saveTips(defaultTips)
        }
    }

    fun getTips(): List<String> {
        val p = prefs ?: return defaultTips
        val json = p.getString(KEY_TIPS, null) ?: return defaultTips
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun saveTips(tips: List<String>) {
        val arr = JSONArray(tips)
        prefs?.edit()?.putString(KEY_TIPS, arr.toString())?.apply()
    }

    /**
     * 获取随机提示语
     */
    fun getRandomTip(): String {
        val tips = getTips()
        return if (tips.isNotEmpty()) tips.random() else "休息一下吧"
    }
}
