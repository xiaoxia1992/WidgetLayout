package com.rexy.example

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rexy.example.extend.BaseFragment
import com.rexy.model.DecorationOffsetLinear
import com.rexy.model.TestRecyclerAdapter
import com.rexy.widgetlayout.example.R

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-07-28 13:32
 */
class FragmentNestFloatLayout : BaseFragment() {

    private val mListView by lazy { view!!.findViewById(R.id.listView) as RecyclerView }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_nestfloatlayout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRecyclerView(mListView, 50)
    }

    private fun initRecyclerView(recyclerView: RecyclerView, initCount: Int) {
        recyclerView.adapter = TestRecyclerAdapter(activity, createData("item", initCount))
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.addItemDecoration(DecorationOffsetLinear(false, 20))
    }

    private fun createData(prefix: String, count: Int): List<String> {
        val list = MutableList(count + 1){
            prefix + " " + (it + 1)
        }
        return list
    }
}