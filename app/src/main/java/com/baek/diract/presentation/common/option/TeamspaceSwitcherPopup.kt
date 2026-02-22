package com.baek.diract.presentation.common.option

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import kotlin.math.min

class TeamspaceSwitcherPopup(
    private val context: Context,
    private val items: List<TeamspaceUi>,
    private val selectedId: String?,
    private val onSelect: (TeamspaceUi) -> Unit,
    private val onCreate: () -> Unit = {}
) {
    private var popup: PopupWindow? = null

    fun show(anchor: View) {
        val content = LayoutInflater.from(context)
            .inflate(R.layout.popup_teamspace_switcher, null, false)

        // 1) 팀스페이스 목록
        val rv = content.findViewById<RecyclerView>(R.id.rvTeamspaces)
        rv.layoutManager = LinearLayoutManager(context)
        val adapter = Adapter(items, selectedId) { selected ->
            dismiss()
            onSelect(selected)
        }
        rv.adapter = adapter

        // 2) 새 팀스페이스 만들기
        content.findViewById<View>(R.id.createTeamspaceRow).setOnClickListener {
            dismiss()
            onCreate()
        }

        // ✅ 3) RV 높이를 아이템 수에 맞게 조절 (최대 460dp)
        content.post {
            adjustRecyclerHeight(rv, adapter.itemCount, maxDp = 460)
            // 높이 바뀐 뒤 popup 위치/폭 계산이 정확해지게 다시 measure
            content.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        val pw = PopupWindow(
            content,
            dp(anchor, 232),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 12f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        popup = pw

        // 측정
        content.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupW = content.measuredWidth
        val screenW = anchor.resources.displayMetrics.widthPixels
        val margin = dp(anchor, 12)

        // anchor 기준 가운데 정렬 + 화면 밖 보정
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val ax = loc[0]
        val anchorCenterX = ax + anchor.width / 2f
        val desiredX = (anchorCenterX - popupW / 2f).toInt()
        val clampedX = desiredX.coerceIn(margin, screenW - popupW - margin)
        val xOff = clampedX - ax

        pw.showAsDropDown(anchor, xOff, dp(anchor, 8))
    }

    fun dismiss() {
        popup?.dismiss()
        popup = null
    }

    private fun adjustRecyclerHeight(rv: RecyclerView, itemCount: Int, maxDp: Int) {
        if (itemCount <= 0) {
            rv.layoutParams = rv.layoutParams.apply { height = 0 }
            return
        }

        val maxPx = dp(rv, maxDp)

        // 아이템 높이가 고정인 경우가 대부분이라 1개 높이만 측정해서 곱함
        val itemHeight = measureOneItemHeight(rv) ?: dp(rv, 56) // fallback
        val padding = rv.paddingTop + rv.paddingBottom
        val desired = itemHeight * itemCount + padding

        rv.layoutParams = rv.layoutParams.apply {
            height = min(desired, maxPx)
        }
        rv.requestLayout()
    }

    private fun measureOneItemHeight(rv: RecyclerView): Int? {
        val ad = rv.adapter ?: return null
        if (ad.itemCount <= 0) return null

        val vh = ad.createViewHolder(rv, ad.getItemViewType(0))
        vh.itemView.measure(
            View.MeasureSpec.makeMeasureSpec(rv.width.takeIf { it > 0 } ?: dp(rv, 232), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val h = vh.itemView.measuredHeight
        return if (h > 0) h else null
    }

    private fun dp(v: View, dp: Int): Int =
        (dp * v.resources.displayMetrics.density).toInt()

    private class Adapter(
        private val items: List<TeamspaceUi>,
        private val selectedId: String?,
        private val onSelect: (TeamspaceUi) -> Unit
    ) : RecyclerView.Adapter<Adapter.ItemVH>() {

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_teamspace_switcher, parent, false)
            return ItemVH(v)
        }

        override fun onBindViewHolder(holder: ItemVH, position: Int) {
            val item = items[position]
            holder.bind(item, item.id == selectedId, onSelect)
        }

        class ItemVH(v: View) : RecyclerView.ViewHolder(v) {
            private val tv = v.findViewById<TextView>(R.id.tvName)
            private val iv = v.findViewById<ImageView>(R.id.ivCheck)

            fun bind(item: TeamspaceUi, selected: Boolean, onSelect: (TeamspaceUi) -> Unit) {
                tv.text = item.name

                val lp = tv.layoutParams as ConstraintLayout.LayoutParams
                if (selected) {
                    iv.visibility = View.VISIBLE
                    lp.startToStart = ConstraintLayout.LayoutParams.UNSET
                    lp.startToEnd = R.id.ivCheck
                    lp.marginStart = dp(tv, 12)
                } else {
                    iv.visibility = View.GONE
                    lp.startToEnd = ConstraintLayout.LayoutParams.UNSET
                    lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    lp.marginStart = 0
                }
                tv.layoutParams = lp

                itemView.setOnClickListener { onSelect(item) }
            }

            private fun dp(v: View, dp: Int): Int =
                (dp * v.resources.displayMetrics.density).toInt()
        }
    }
}

data class TeamspaceUi(val id: String, val name: String)