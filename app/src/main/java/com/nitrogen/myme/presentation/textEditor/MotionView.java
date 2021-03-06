package com.nitrogen.myme.presentation.textEditor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import com.nitrogen.myme.presentation.GestureDetector;

import java.util.ArrayList;
import java.util.List;

import com.nitrogen.myme.R;

public class MotionView extends FrameLayout {

    public interface Constants {
        float SELECTED_LAYER_ALPHA = 0.15F;
    }

    public interface MotionViewCallback {
        void onTextEntitySelected(@Nullable TextEntity textEntity);
        void onTextEntityDoubleTap(@NonNull TextEntity textEntity);
    }

    // layers
    private final List<TextEntity> entities = new ArrayList<>();
    @Nullable
    private TextEntity selectedTextEntity;

    private Paint selectedLayerPaint;

    // callback
    @Nullable
    private MotionViewCallback motionViewCallback;

    // gesture detection
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private GestureDetectorCompat gestureDetectorCompat;

    // constructors
    public MotionView(Context context) {
        super(context);
        init(context);
    }

    public MotionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MotionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MotionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(@NonNull Context context) {
        setWillNotDraw(false);

        selectedLayerPaint = new Paint();
        selectedLayerPaint.setAlpha((int) (255 * Constants.SELECTED_LAYER_ALPHA));
        selectedLayerPaint.setAntiAlias(true);

        // init listeners
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        this.gestureDetector = new GestureDetector(new MoveListener());
        this.gestureDetectorCompat = new GestureDetectorCompat(context, new TapsListener());

        setOnTouchListener(onTouchListener);

        updateUI();
    }

    public @Nullable TextEntity getSelectedTextEntity() {
        return selectedTextEntity;
    }

    public List<TextEntity> getTextEntities() {
        return entities;
    }

    public void setMotionViewCallback(@Nullable MotionViewCallback callback) {
        this.motionViewCallback = callback;
    }

    public void addTextEntity(@Nullable TextEntity textEntity) {
        if (textEntity != null) {
            entities.add(textEntity);
            selectTextEntity(textEntity, false);
        }
    }

    public void addEntityAndPosition(@Nullable TextEntity textEntity) {
        if (textEntity != null) {
            initEntityBorder(textEntity);
            initialTranslateAndScale(textEntity);
            entities.add(textEntity);
            selectTextEntity(textEntity, true);
        }
    }

    private void initEntityBorder(@NonNull TextEntity textEntity ) {
        // init stroke
        int strokeSize = getResources().getDimensionPixelSize(R.dimen.stroke_size);
        Paint borderPaint = new Paint();
        borderPaint.setStrokeWidth(strokeSize);
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.stroke_color));

        textEntity.setBorderPaint(borderPaint);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        // dispatch draw is called after child views is drawn.
        // the idea that is we draw background stickers, than child views (if any), and than selected item
        // to draw on top of child views - do it in dispatchDraw(Canvas)
        // to draw below that - do it in onDraw(Canvas)
        if (selectedTextEntity != null) {
            selectedTextEntity.draw(canvas, selectedLayerPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawAllEntities(canvas);
        super.onDraw(canvas);
    }

    /* drawAllEntities
     *
     * purpose: draws all entities on the canvas
     */
    private void drawAllEntities(Canvas canvas) {
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).draw(canvas, null);
        }
    }

    /* getThumbnailImage
     *
     * Note: as a side effect - the method deselects Entity (if any selected)
     * purpose: return bitmap with all the Entities at their current positions
     */
    public Bitmap getThumbnailImage() {
        selectTextEntity(null, false);

        Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        // IMPORTANT: always create white background, cos if the image is saved in JPEG format,
        // which doesn't have transparent pixels, the background will be black
        bmp.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bmp);
        drawAllEntities(canvas);

        return bmp;
    }

    private void updateUI() {
        invalidate();
    }

    private void handleTranslate(PointF delta) {
        if (selectedTextEntity != null) {
            float newCenterX = selectedTextEntity.absoluteCenterX() + delta.x;
            float newCenterY = selectedTextEntity.absoluteCenterY() + delta.y;
            // limit entity center to screen bounds
            boolean needUpdateUI = false;
            if (newCenterX >= 0 && newCenterX <= getWidth()) {
                selectedTextEntity.getTextLayer().postTranslate(delta.x / getWidth(), 0.0F);
                needUpdateUI = true;
            }
            if (newCenterY >= 0 && newCenterY <= getHeight()) {
                selectedTextEntity.getTextLayer().postTranslate(0.0F, delta.y / getHeight());
                needUpdateUI = true;
            }
            if (needUpdateUI) {
                updateUI();
            }
        }
    }

    private void initialTranslateAndScale(@NonNull TextEntity textEntity) {
        textEntity.moveToCanvasCenter();
        textEntity.getTextLayer().setScale(textEntity.getTextLayer().initialScale());
    }

    private void selectTextEntity(@Nullable TextEntity textEntity, boolean updateCallback) {
        if (selectedTextEntity != null) {
            selectedTextEntity.setIsSelected(false);
        }
        if (textEntity != null) {
            textEntity.setIsSelected(true);
        }
        selectedTextEntity = textEntity;
        invalidate();
        if (updateCallback && motionViewCallback != null) {
            motionViewCallback.onTextEntitySelected(textEntity);
        }
    }

    public void unselectEntity() {
        if (selectedTextEntity != null) {
            selectTextEntity(null, true);
        }
    }

    @Nullable
    private TextEntity findEntityAtPoint(float x, float y) {
        TextEntity selected = null;
        PointF p = new PointF(x, y);
        for (int i = entities.size() - 1; i >= 0; i--) {
            if (entities.get(i).pointInLayerRect(p)) {
                selected = entities.get(i);
                break;
            }
        }
        return selected;
    }

    private void updateSelectionOnTap(MotionEvent e) {
        TextEntity textEntity = findEntityAtPoint(e.getX(), e.getY());
        selectTextEntity(textEntity, true);
    }

    private void updateOnLongPress(MotionEvent e) {
        // if layer is currently selected and point inside layer - move it to front
        if (selectedTextEntity != null) {
            PointF p = new PointF(e.getX(), e.getY());
            if (selectedTextEntity.pointInLayerRect(p)) {
                bringLayerToFront(selectedTextEntity);
            }
        }
    }

    private void bringLayerToFront(@NonNull TextEntity textEntity) {
        // removing and adding brings layer to front
        if (entities.remove(textEntity)) {
            entities.add(textEntity);
            invalidate();
        }
    }

    private void moveEntityToBack(@Nullable TextEntity textEntity) {
        if (textEntity == null) {
            return;
        }
        if (entities.remove(textEntity)) {
            entities.add(0, textEntity);
            invalidate();
        }
    }

    public void flipSelectedTextEntity() {
        if (selectedTextEntity == null) {
            return;
        }
        selectedTextEntity.getTextLayer().flip();
        invalidate();
    }

    public void moveSelectedBack() {
        moveEntityToBack(selectedTextEntity);
    }

    public void deletedSelectedTextEntity() {
        if (selectedTextEntity == null) {
            return;
        }
        if (entities.remove(selectedTextEntity)) {
            selectedTextEntity.release();
            selectedTextEntity = null;
            invalidate();
        }
    }

    // memory
    public void release() {
        for (TextEntity textEntity : entities) {
            textEntity.release();
        }
        entities.clear();
        selectedTextEntity = null;
    }

    // gesture detectors
    private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (scaleGestureDetector != null) {
                scaleGestureDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);
                gestureDetectorCompat.onTouchEvent(event);
            }
            return true;
        }
    };

    private class TapsListener extends android.view.GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (motionViewCallback != null && selectedTextEntity != null) {
                motionViewCallback.onTextEntityDoubleTap(selectedTextEntity);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            updateOnLongPress(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            updateSelectionOnTap(e);
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (selectedTextEntity != null) {
                float scaleFactorDiff = detector.getScaleFactor();
                selectedTextEntity.getTextLayer().postScale(scaleFactorDiff - 1.0F);
                updateUI();
            }
            return true;
        }
    }

    private class MoveListener extends GestureDetector.SimpleOnMoveGestureListener {
        @Override
        public boolean onMove(GestureDetector detector) {
            handleTranslate(detector.getFocusDelta());
            return true;
        }
    }
}
