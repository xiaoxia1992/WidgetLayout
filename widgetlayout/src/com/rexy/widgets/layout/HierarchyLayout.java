package com.rexy.widgets.layout;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;

import com.rexy.widgets.ViewHelper;
import com.rexy.widgets.view.ViewHierarchyInfo;
import com.rexy.widgets.view.ViewHierarchyTree;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.Style.STROKE;
import static android.graphics.Typeface.NORMAL;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.INVALID_POINTER_ID;

public class HierarchyLayout extends WrapLayout {
    private static final int TRACKING_UNKNOWN = 0;
    private static final int TRACKING_VERTICALLY = 1;
    private static final int TRACKING_HORIZONTALLY = -1;
    private static final int ROTATION_MAX = 55;
    private static final int ROTATION_MIN = -ROTATION_MAX;
    private static final int ROTATION_DEFAULT_X = 6;
    private static final int ROTATION_DEFAULT_Y = -12;
    private static final float ZOOM_DEFAULT = 0.75f;
    private static final float ZOOM_MIN = 0.5f;
    private static final float ZOOM_MAX = 1.5f;
    private static final int SPACING_DEFAULT = 25;
    private static final int SPACING_MIN = 10;
    private static final int SPACING_MAX = 100;

    Rect mLayoutBounds = new Rect();
    Rect mTempRect = new Rect();
    PointF mTempPointF = new PointF();
    Resources mResources;
    float mSlop, mDensity;


    private final RectF mOptionRect = new RectF();
    private final Rect mViewBounds = new Rect();
    private final Paint mViewBorderPaint = new Paint(ANTI_ALIAS_FLAG);
    private final Camera mCamera = new Camera();
    private final Matrix mMatrix = new Matrix();

    private final SparseArray<String> mIdNameArr = new SparseArray<>();

    private float mViewTextOffset = 1;
    private boolean mHierarchyViewEnable = true;
    private boolean mHierarchyNodeEnable = true;
    private boolean mHierarchySummaryEnable = true;
    private boolean mDrawViewEnable = true;

    private boolean mDrawViewIdEnable = false;
    private int mPointerOne = INVALID_POINTER_ID;
    private PointF mLastPointOne = new PointF();
    private int mPointerTwo = INVALID_POINTER_ID;
    private PointF mLastPointTwo = new PointF();

    private PointF mPointDown = new PointF();

    private int mMultiTouchTracking = TRACKING_UNKNOWN;
    private float mRotationY = ROTATION_DEFAULT_Y;
    private float mRotationX = ROTATION_DEFAULT_X;
    private float mZoom = ZOOM_DEFAULT;

    private float mSpacing = SPACING_DEFAULT;
    private int mViewColor = 0xFF888888;


    private int mViewShadowColor = 0xFF000000;
    private int mHierarchyColor = 0xAA000000;

    private int mNodeLeafStrokeColor = 0xFFFFFFFF;
    private int mTreeNodeColor = 0xFF00FF00;
    private int mTreeLeafColor = 0xFFFF0000;
    private int mTreeBranchColor = 0xFFFFFFFF;

    private int mTreeBackground = 0;
    private int mTreeTextSize = 4;
    private int mTreeTextColor = 0xFFFF0000;
    private int mTreeSumTextSize = 15;

    private int mTreeSumTextColor = 0xFFAA2A20;
    private int mMaxTreeLeafSize = -1;

    private float mTreeWidthWeight = 0.95f;
    private float mTreeHeightWeight = 0.85f;
    private float mTreeLeafMarginWeight = 1f;
    private float mTreeLevelMarginWeight = 3.5f;
    private float mTreeOffsetX = 0;
    private float mTreeOffsetY = 10;

    float mLeafSize;
    float mLeafMargin;
    float mLevelMargin;
    boolean mHierarchyTreeHorizontal;
    ViewHierarchyTree mTree;
    StringBuilder mStringBuilder = new StringBuilder();
    Paint mTreePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HierarchyLayout(Context context) {
        super(context);
        init(context, null);
    }

    public HierarchyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HierarchyLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mResources = context.getResources();
        mDensity = context.getResources().getDisplayMetrics().density;
        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTreePaint.setStyle(Paint.Style.FILL);
        mTreePaint.setTextAlign(Paint.Align.CENTER);
        mTreeOffsetX *= mDensity;
        mTreeOffsetY *= mDensity;

        mViewTextOffset *= mDensity;
        mViewBorderPaint.setStyle(STROKE);
        mViewBorderPaint.setTextSize(6 * mDensity);
        if (Build.VERSION.SDK_INT >= JELLY_BEAN) {
            mViewBorderPaint.setTypeface(Typeface.create("sans-serif-condensed", NORMAL));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mHierarchyViewEnable) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                requestDisallowInterceptTouchEvent(true);
            }
        } else {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mPointDown.set(ev.getX(), ev.getY());
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                handleClickUp(ev.getX(), ev.getY());
            }
        }
        return mHierarchyViewEnable || super.onInterceptTouchEvent(ev);
    }

    private void handleClickUp(float endX, float endY) {
        float x = mPointDown.x, y = mPointDown.y;
        if (Math.abs(x - endX) < mSlop && Math.abs(y - endY) < mSlop) {
            if (x >= mOptionRect.left - mSlop && x <= mOptionRect.right + mSlop && y >= mOptionRect.top - mSlop && y <= mOptionRect.bottom + mSlop) {
                if (x > mOptionRect.centerX()) {
                    mHierarchyViewEnable = !mHierarchyViewEnable;
                } else {
                    mHierarchyNodeEnable = !mHierarchyNodeEnable;
                }
                invalidate();
            }
        }
    }

    @Override
    public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {
        int action = event.getActionMasked();
        if (!mHierarchyViewEnable) {
            if (action == MotionEvent.ACTION_DOWN) {
                mPointDown.set(event.getX(), event.getY());
                if (mPointDown.x >= mOptionRect.left - mSlop && mPointDown.x <= mOptionRect.right + mSlop && mPointDown.y >= mOptionRect.top - mSlop && mPointDown.y <= mOptionRect.bottom + mSlop) {
                    return true;
                }
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                handleClickUp(event.getX(), event.getY());
            }
            return super.onTouchEvent(event);
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mPointDown.set(event.getX(), event.getY());
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = (action == ACTION_DOWN) ? 0 : event.getActionIndex();
                if (mPointerOne == INVALID_POINTER_ID) {
                    mPointerOne = event.getPointerId(index);
                    mLastPointOne.set(event.getX(index), event.getY(index));
                } else if (mPointerTwo == INVALID_POINTER_ID) {
                    mPointerTwo = event.getPointerId(index);
                    mLastPointTwo.set(event.getX(index), event.getY(index));
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mPointerTwo == INVALID_POINTER_ID) {
                    // Single pointer controlling 3D rotation.
                    for (int i = 0, count = event.getPointerCount(); i < count; i++) {
                        if (mPointerOne == event.getPointerId(i)) {
                            float eventX = event.getX(i);
                            float eventY = event.getY(i);
                            float drx = 90 * ((eventX - mLastPointOne.x) / getWidth());
                            float dry = 90 * (-(eventY - mLastPointOne.y) / getHeight()); // Invert Y-axis.
                            // An 'x' delta affects 'y' rotation and vise versa.
                            if (drx != 0 || dry != 0) {
                                mRotationY = Math.min(Math.max(mRotationY + drx, ROTATION_MIN), ROTATION_MAX);
                                mRotationX = Math.min(Math.max(mRotationX + dry, ROTATION_MIN), ROTATION_MAX);
                                mLastPointOne.set(eventX, eventY);
                                invalidate();
                            }
                        }
                    }
                } else {
                    int pointerOneIndex = event.findPointerIndex(mPointerOne);
                    int pointerTwoIndex = event.findPointerIndex(mPointerTwo);
                    float xOne = event.getX(pointerOneIndex);
                    float yOne = event.getY(pointerOneIndex);
                    float xTwo = event.getX(pointerTwoIndex);
                    float yTwo = event.getY(pointerTwoIndex);
                    float dxOne = xOne - mLastPointOne.x;
                    float dyOne = yOne - mLastPointOne.y;
                    float dxTwo = xTwo - mLastPointTwo.x;
                    float dyTwo = yTwo - mLastPointTwo.y;
                    if (mMultiTouchTracking == TRACKING_UNKNOWN) {
                        float adx = Math.abs(dxOne) + Math.abs(dxTwo);
                        float ady = Math.abs(dyOne) + Math.abs(dyTwo);
                        if (adx > mSlop * 2 || ady > mSlop * 2) {
                            if (adx > ady) {
                                // Left/right movement wins. Track horizontal.
                                mMultiTouchTracking = TRACKING_HORIZONTALLY;
                            } else {
                                // Up/down movement wins. Track vertical.
                                mMultiTouchTracking = TRACKING_VERTICALLY;
                            }
                        }
                    }
                    if (mMultiTouchTracking != TRACKING_UNKNOWN) {
                        if (dyOne != dyTwo) {
                            if (mMultiTouchTracking == TRACKING_VERTICALLY) {
                                if (yOne >= yTwo) {
                                    mZoom += dyOne / getHeight() - dyTwo / getHeight();
                                } else {
                                    mZoom += dyTwo / getHeight() - dyOne / getHeight();
                                }
                                mZoom = Math.min(Math.max(mZoom, ZOOM_MIN), ZOOM_MAX);

                            }
                            if (mMultiTouchTracking == TRACKING_HORIZONTALLY) {
                                if (xOne >= xTwo) {
                                    mSpacing += (dxOne / getWidth() * SPACING_MAX) - (dxTwo / getWidth() * SPACING_MAX);
                                } else {
                                    mSpacing += (dxTwo / getWidth() * SPACING_MAX) - (dxOne / getWidth() * SPACING_MAX);
                                }
                                mSpacing = Math.min(Math.max(mSpacing, SPACING_MIN), SPACING_MAX);
                            }
                            invalidate();
                        }
                        mLastPointOne.set(xOne, yOne);
                        mLastPointTwo.set(xTwo, yTwo);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleClickUp(event.getX(), event.getY());
            case MotionEvent.ACTION_POINTER_UP: {
                int index = (action != ACTION_POINTER_UP) ? 0 : event.getActionIndex();
                int pointerId = event.getPointerId(index);
                if (mPointerOne == pointerId) {
                    // Shift pointer two (real or invalid) up to pointer one.
                    mPointerOne = mPointerTwo;
                    mLastPointOne.set(mLastPointTwo);
                    // Clear pointer two and tracking.
                    mPointerTwo = INVALID_POINTER_ID;
                    mMultiTouchTracking = TRACKING_UNKNOWN;
                } else if (mPointerTwo == pointerId) {
                    mPointerTwo = INVALID_POINTER_ID;
                    mMultiTouchTracking = TRACKING_UNKNOWN;
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void doAfterMeasure(int measuredWidth, int measuredHeight, int contentWidth, int contentHeight) {
        super.doAfterMeasure(measuredWidth, measuredHeight, contentWidth, contentHeight);
        mLayoutBounds.set(0, 0, measuredWidth, measuredHeight);
        mOptionRect.setEmpty();
    }

    @Override
    protected void doAfterLayout(int contentLeft, int contentTop, int contentWidth, int contentHeight, boolean firstAttachLayout) {
        if (mTree != null) {
            mTree.destroy();
        }
        mLeafSize = -1;
        mTree = ViewHelper.getViewHierarchyOf(this);
        int width = mLayoutBounds.width(), height = mLayoutBounds.height();
        mHierarchyTreeHorizontal = width > height;
        int longSize = Math.max(width, height), shortSize = Math.min(width, height);
        calculateHierarchyLayoutRadius(longSize * mTreeWidthWeight, shortSize * mTreeHeightWeight, mHierarchyTreeHorizontal);
    }

    @Override
    protected void doAfterDraw(Canvas canvas, int contentLeft, int contentTop, int contentWidth, int contentHeight) {
        if (mTree != null && mLeafSize > 0) {
            if ((mHierarchyNodeEnable || mHierarchyViewEnable)) {
                if (mHierarchyColor != 0) {
                    canvas.drawColor(mHierarchyColor);
                }
                if (mHierarchyViewEnable) {
                    drawHierarchyView(canvas);
                }
                if (mHierarchyNodeEnable) {
                    drawHierarchyTree(canvas);
                }
            }
            mTreePaint.setColor(0xAA00FF00);
            canvas.drawRect(0, mOptionRect.top - mDensity * 3, mLayoutBounds.right, mOptionRect.bottom + mDensity * 3, mTreePaint);
            mTreePaint.setColor(mTreeSumTextColor);
            mTreePaint.setTextSize(mTreeSumTextSize * mDensity);
            mTreePaint.setTextAlign(Paint.Align.LEFT);
            if (mHierarchySummaryEnable && mTreeSumTextColor != 0 && mTreeSumTextSize > 0) {
                RectF treeBounds = mTree.getTag();
                drawTreeSummaryInfo(canvas, treeBounds, mHierarchyTreeHorizontal);
            }
            drawOptionBar(canvas);
        }
    }

    private void drawHierarchyView(Canvas canvas) {
        boolean applyChangeVisible = mHierarchyColor != 0 && (255 == (mHierarchyColor >>> 24));
        Rect location = mTree.getWindowLocation();
        int saveCount = canvas.save();
        final float x = location.left, y = location.top;
        final float translateShowX = mSpacing * mDensity * mRotationY / ROTATION_MAX;
        final float translateShowY = mSpacing * mDensity * mRotationX / ROTATION_MAX;
        final float cx = getWidth() / 2f, cy = getHeight() / 2f;
        mCamera.save();
        mCamera.rotate(mRotationX, mRotationY, 0);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();
        mMatrix.preTranslate(-cx, -cy);
        mMatrix.postTranslate(cx, cy);
        canvas.concat(mMatrix);
        canvas.scale(mZoom, mZoom, cx, cy);
        SparseArray<ViewHierarchyInfo> nodes = mTree.getHierarchyNodeArray();
        mViewBorderPaint.setColor(mViewColor);
        mViewBorderPaint.setShadowLayer(0, 1, -1, mViewShadowColor);
        for (int i = 1; i < nodes.size(); i++) {
            ViewHierarchyInfo node = nodes.get(i);
            View view = node.getView();
            int layer = node.getLevel();
            int viewSaveCount = canvas.save();
            float tx = layer * translateShowX, ty = layer * translateShowY;
            location = node.getWindowLocation();
            canvas.translate(tx, -ty);
            canvas.translate(location.left - x, location.top - y);
            mViewBounds.set(0, 0, view.getWidth(), view.getHeight());
            canvas.drawRect(mViewBounds, mViewBorderPaint);
            if (mDrawViewEnable) {
                if (applyChangeVisible) {
                    changeChildVisible(view, true);
                    view.draw(canvas);
                    changeChildVisible(view, false);
                } else {
                    boolean viewGroupType = view instanceof ViewGroup;
                    if (viewGroupType) {
                        if (view.getBackground() != null) {
                            view.getBackground().draw(canvas);
                        }
                    } else {
                        view.draw(canvas);
                    }
                }
            }
            if (mDrawViewIdEnable) {
                int id = view.getId();
                if (id != NO_ID) {
                    canvas.drawText(nameForId(id), mViewTextOffset, mViewBorderPaint.getTextSize(), mViewBorderPaint);
                }
            }
            canvas.restoreToCount(viewSaveCount);
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawOptionBar(Canvas canvas) {
        float textHeight = mTreePaint.descent() - mTreePaint.ascent();
        if (mOptionRect.isEmpty()) {
            mOptionRect.set(mLayoutBounds);
            mOptionRect.top = mOptionRect.bottom - textHeight;
            mOptionRect.left = mOptionRect.right - textHeight * 5;
            mOptionRect.offset(-mDensity * 3, -textHeight);
            postInvalidate();
        }
        float midX = mOptionRect.centerX();
        float d = mTreePaint.descent();
        float a = mTreePaint.ascent();
        float baseY = mOptionRect.centerY() + (d - a) / 2 - d;
        canvas.drawLine(midX, mOptionRect.top, midX, mOptionRect.bottom, mViewBorderPaint);
        canvas.drawText("NODE", mOptionRect.left, baseY, mTreePaint);
        canvas.drawText("VIEW", mOptionRect.right - calculateTextBounds("VIEW", mTreePaint, mTempRect).width() - mDensity, baseY, mTreePaint);

        mTreePaint.setColor(mHierarchyNodeEnable ? 0x880000FF : 0x220000FF);
        canvas.drawRect(mOptionRect.left, mOptionRect.top, midX - mDensity, mOptionRect.bottom, mTreePaint);

        mTreePaint.setColor(mHierarchyViewEnable ? 0x880000FF : 0x220000FF);
        canvas.drawRect(midX + mDensity, mOptionRect.top, mOptionRect.right, mOptionRect.bottom, mTreePaint);
    }

    private void drawTreeSummaryInfo(Canvas canvas, RectF treeBounds, boolean horizontal) {
        mStringBuilder.delete(0, mStringBuilder.length());
        mStringBuilder.append("层级(").append(mTree.getHierarchyCount()).append(',').append(String.format("%.1f", mTree.getArgHierarchyCount())).append(")").append(',');
        mStringBuilder.append("结点(").append(mTree.getCountOfNode()).append(',').append(mTree.getCountOfViewGroup()).append(',').append(mTree.getCountOfView()).append(")");
        float textHeight = mTreePaint.descent() - mTreePaint.ascent();
        float d = mTreePaint.descent();
        float a = mTreePaint.ascent();
        canvas.drawText(mStringBuilder.toString(), textHeight / 2, mOptionRect.centerY() + (d - a) / 2 - d, mTreePaint);
    }

    protected void drawTreeNode(Canvas canvas, ViewHierarchyInfo info, float radius) {
        PointF tempPoint = mTempPointF;
        ViewHierarchyInfo parent = info.getParent();
        getNodePosition((RectF) info.getTag(), tempPoint, radius);
        float x = tempPoint.x, y = tempPoint.y;
        if (parent != null && mTreeBranchColor != 0) {
            getNodePosition((RectF) parent.getTag(), tempPoint, radius);
            float px = tempPoint.x, py = tempPoint.y;
            mTreePaint.setColor(mTreeBranchColor);
            canvas.drawLine(x, y, px, py, mTreePaint);
        }
        mTreePaint.setColor(mNodeLeafStrokeColor);
        canvas.drawCircle(x, y, radius + mDensity, mTreePaint);

        mTreePaint.setColor(info.isLeaf() ? mTreeLeafColor : mTreeNodeColor);
        canvas.drawCircle(x, y, radius, mTreePaint);

        if (mTreeTextColor != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(info.getMarkName()).append('[').append(info.getLevel()).append(',').append(info.getLevelIndex()).append(']');
            canvas.drawText(sb.toString(), x, y - radius - radius, mTreePaint);
        }
    }

    protected void drawHierarchyTree(Canvas canvas) {
        if (mLeafSize > 0) {
            mTreePaint.setTextAlign(Paint.Align.CENTER);
            RectF treeBounds = mTree.getTag();
            if (mTreeBackground != 0) {
                mTreePaint.setColor(mTreeBackground);
                canvas.drawRect(treeBounds, mTreePaint);
            }
            if (mTreeTextColor != 0) {
                mTreePaint.setTextSize(mTreeTextSize * mDensity);
            }
            float radius = mLeafSize / 2f;
            SparseArray<ViewHierarchyInfo> infoArr = mTree.getHierarchyNodeArray();
            int size = infoArr.size();
            for (int i = size - 1; i >= 0; i--) {
                drawTreeNode(canvas, infoArr.get(i), radius);
            }
        }
    }

    private final SparseArray<View> mTempInt = new SparseArray();

    private void changeChildVisible(View view, boolean hide) {
        if (view instanceof ViewGroup) {
            ViewGroup p = (ViewGroup) view;
            int size = p.getChildCount();
            if (size > 0) {
                if (hide) {
                    mTempInt.clear();
                    for (int i = 0; i < size; i++) {
                        View child = p.getChildAt(i);
                        if (child.getVisibility() == View.VISIBLE) {
                            mTempInt.put(i, child);
                            child.setVisibility(View.INVISIBLE);
                        }
                    }
                } else {
                    size = mTempInt.size();
                    for (int i = 0; i < size; i++) {
                        mTempInt.valueAt(i).setVisibility(View.VISIBLE);
                    }
                    mTempInt.clear();
                }
            }
        }
    }

    private Rect calculateTextBounds(String text, Paint paint, Rect result) {
        result.setEmpty();
        if (text != null) {
            paint.getTextBounds(text, 0, text.length(), result);
        }
        return result;
    }

    private void calculateHierarchyLayoutRadius(float longSize, float shortSize, boolean horizontal) {
        int leafCount = mTree.getLeafCount();
        int levelCount = mTree.getHierarchyCount();
        if (leafCount > 0 && levelCount > 0) {
            //leafCount*leafSize+(leafCount-1)*(leafSize*mLeafMarginHorizontalFactor)=w;
            mLeafSize = (longSize / (leafCount + (leafCount - 1) * mTreeLeafMarginWeight));
            if (mMaxTreeLeafSize > 0 && mLeafSize > mMaxTreeLeafSize) {
                mLeafSize = mMaxTreeLeafSize;
            }
            mLeafMargin = (mTreeLeafMarginWeight * mLeafSize);
            mLevelMargin = (mTreeLevelMarginWeight * mLeafSize);
            //leafLevel*leafSize+(leafCount-1)*maxMarginVertical=h;
            if (levelCount > 0) {
                mLevelMargin = Math.min(mLevelMargin, (shortSize - levelCount * mLeafSize) / (levelCount - 1));
            }
            float hierarchyWidth = leafCount * mLeafSize + (leafCount - 1) * mLeafMargin;
            float hierarchyHeight = levelCount * mLeafSize + (levelCount - 1) * mLevelMargin;
            calculateHierarchyLayoutPosition(mLayoutBounds, hierarchyWidth, hierarchyHeight, horizontal);
        }
    }

    private void calculateHierarchyLayoutPosition(Rect canvasBounds, float hierarchyWidth, float hierarchyHeight, boolean horizontal) {
        RectF rootBounds = new RectF();
        if (horizontal) {
            rootBounds.left = (int) (canvasBounds.left + (1 - mTreeWidthWeight) * canvasBounds.width() / 2);
            rootBounds.top = (int) (canvasBounds.top + (1 - mTreeHeightWeight) * canvasBounds.height() / 2);
            rootBounds.right = rootBounds.left + hierarchyWidth;
            rootBounds.bottom = rootBounds.top + hierarchyHeight;
        } else {
            rootBounds.left = (int) (canvasBounds.left + (1 - mTreeHeightWeight) * canvasBounds.width() / 2);
            rootBounds.top = (int) (canvasBounds.top + (1 - mTreeWidthWeight) * canvasBounds.height() / 2);
            rootBounds.right = rootBounds.left + hierarchyHeight;
            rootBounds.bottom = rootBounds.top + hierarchyWidth;
        }
        rootBounds.offset(mTreeOffsetX, mTreeOffsetY);
        SparseIntArray lines = mTree.getHierarchyCountArray();
        SparseArray<ViewHierarchyInfo> list = mTree.getHierarchyNodeArray();

        int lineCount = lines.size(), startIndex, endIndex = 0;
        float levelMargin = mLevelMargin + mLeafSize, usedWeight = 0;
        list.get(endIndex++).setTag(rootBounds);
        ViewHierarchyInfo prevParent = null, parent, child;
        for (int line = 1; line < lineCount; line++) {
            startIndex = endIndex;
            endIndex = startIndex + lines.get(line);
            for (int i = startIndex; i < endIndex; i++) {
                child = list.get(i);
                parent = child.getParent();
                if (parent != prevParent) {
                    usedWeight = 0;
                    prevParent = parent;
                }
                usedWeight += buildAndSetHierarchyBounds(child, parent, usedWeight, levelMargin, horizontal);
            }
        }
        invalidate();
    }

    private int buildAndSetHierarchyBounds(ViewHierarchyInfo child, ViewHierarchyInfo parent, float usedWeight, float levelMargin, boolean horizontal) {
        RectF bounds = new RectF((RectF) parent.getTag());
        int currentWeight = child.getLeafCount();
        float weightSum = parent.getLeafCount(), weightStart = usedWeight, weightEnd = usedWeight + currentWeight;
        float start, end, size;
        if (horizontal) {
            size = bounds.width();
            start = bounds.left + size * weightStart / weightSum;
            end = bounds.left + size * weightEnd / weightSum;
            bounds.left = start;
            bounds.right = end;
            bounds.top = bounds.top + levelMargin;
        } else {
            size = bounds.height();
            start = bounds.bottom - size * weightStart / weightSum;
            end = bounds.bottom - size * weightEnd / weightSum;
            bounds.top = end;
            bounds.bottom = start;
            bounds.left = bounds.left + levelMargin;
        }
        child.setTag(bounds);
        return currentWeight;
    }

    private void getNodePosition(RectF rect, PointF point, float radius) {
        if (mHierarchyTreeHorizontal) {
            point.set(rect.centerX(), rect.top + radius);
        } else {
            point.set(rect.left + radius, rect.centerY());
        }
    }

    private String nameForId(int id) {
        String name = mIdNameArr.get(id);
        if (name == null) {
            try {
                name = mResources.getResourceEntryName(id);
            } catch (Resources.NotFoundException e) {
                name = String.format("0x%8x", id);
            }
            mIdNameArr.put(id, name);
        }
        return name;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mTree != null) {
            mTree.destroy();
            mTree = null;
        }
        mIdNameArr.clear();
    }

    public static boolean isHierarchyInstalled(Activity activity) {
        Window window = activity.getWindow();
        ViewGroup decorView = (ViewGroup) window.getDecorView();
        ViewGroup parent = (ViewGroup) decorView.findViewById(android.R.id.content);
        return parent instanceof HierarchyLayout;
    }

    public static void hierarchy(Activity activity, boolean install) {
        Window window = activity.getWindow();
        ViewGroup decorView = (ViewGroup) window.getDecorView();
        ViewGroup parent = (ViewGroup) decorView.findViewById(android.R.id.content);
        if (install) {
            if (!(parent instanceof HierarchyLayout)) {
                SparseArray<View> childs = new SparseArray();
                int count = parent.getChildCount();
                for (int i = 0; i < count; i++) {
                    childs.put(i, parent.getChildAt(i));
                }
                parent.removeAllViews();
                parent.setId(NO_ID);
                HierarchyLayout hierarchy = new HierarchyLayout(activity);
                hierarchy.setId(android.R.id.content);
                for (int i = 0; i < count; i++) {
                    hierarchy.addView(childs.get(i));
                }
                parent.addView(hierarchy);
            }
        } else {
            if (parent instanceof HierarchyLayout) {
                SparseArray<View> childs = new SparseArray();
                int count = parent.getChildCount();
                for (int i = 0; i < count; i++) {
                    childs.put(i, parent.getChildAt(i));
                }
                parent.removeAllViews();
                parent.setId(NO_ID);
                ViewGroup realParent = (ViewGroup) parent.getParent();
                realParent.setId(android.R.id.content);
                realParent.removeAllViews();
                for (int i = 0; i < count; i++) {
                    realParent.addView(childs.get(i));
                }
            }
        }
    }
}