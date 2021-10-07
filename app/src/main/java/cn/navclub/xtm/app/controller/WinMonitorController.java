package cn.navclub.xtm.app.controller;

import cn.navclub.xtm.app.base.AbstractWindowFXMLController;

import cn.navclub.xtm.app.config.XTApp;
import cn.navclub.xtm.app.event.WinDragEvent;
import cn.navclub.xtm.app.util.FFmpegUtil;
import cn.navclub.xtm.app.util.UIUtil;
import cn.navclub.xtm.kit.proxy.impl.FFmpegFrameGrabberProxy;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.bytedeco.javacv.Frame;
import org.controlsfx.control.Notifications;

/**
 * 远程操作主窗口
 *
 * @author yangkui
 */
public class WinMonitorController extends AbstractWindowFXMLController<BorderPane> {

    @FXML
    private HBox topBox;
    @FXML
    private Canvas canvas;
    @FXML
    private Label robotInfo;

    private final FFmpegFrameGrabberProxy fProxy;



    public WinMonitorController(final Integer robotId, final double width, double height) {
        super("WinMonitorView.fxml");


        this.robotInfo.setText("远程控制 "+robotId);

        WinDragEvent.register(getStage(),this.topBox);

        var rect = UIUtil.getSrnSize();
        //设置当前窗口大小
        this.getStage().setResizable(false);
        this.getStage().setWidth(rect.getWidth()*.9);
        this.getStage().setHeight(rect.getHeight()*.9);
        this.getStage().initStyle(StageStyle.UNDECORATED);


        this.canvas.addEventFilter(MouseEvent.ANY, this::filterMouseEvent);

        //初始化FFMpeg
        this.fProxy = FFmpegFrameGrabberProxy.createGraProxy();
        this.asyncInit(robotId,width,height);
    }

    /**
     * 异步初始化{@link FFmpegFrameGrabberProxy} 代理类
     */
    private void asyncInit(int robotId,double width,double height) {

        var filename = String.format(
                "rtmp://%s/myapp?robotId=%d",
                XTApp.getInstance().getHost(),
                robotId
        );

        var future = this.fProxy
                .setCallback(this::onReceive)
                .setFilename(filename)
                .setImgWidth((int) width)
                .setImgHeight((int) height)
                .setFormat("flv")
                .asyncStart();

        future.whenComplete((r, t) -> {
            if (t != null) {
                Platform.runLater(() -> {
                    Notifications.create().text("获取数据流失败!").showError();
                    this.triggerClose(false);
                    MainViewController.newInstance().openWindow();
                });
            }
        });
    }

    /**
     * 捕获画布上的鼠标事件并转发至被控制终端
     */
    private void filterMouseEvent(MouseEvent event) {

    }


    /**
     * 刷新视图
     */
    private void onReceive(Frame frame) {
        var wi = FFmpegUtil.toFXImage(frame);
        Platform.runLater(() -> {
            var width = this.canvas.getWidth();
            var height = this.canvas.getHeight();
            var context = this.canvas.getGraphicsContext2D();
            context.clearRect(0, 0, width, height);
            context.drawImage(wi, 0, 0, width, height);
        });
    }

    @FXML
    private void requestClose(){
        this.triggerClose(false);
    }

    @FXML
    private void requestIconified(){
        this.getStage().setIconified(true);
    }


    @Override
    public void stageShowChange(boolean oldValue, boolean newValue) {
        if (!newValue){
            return;
        }
        this.canvas.setHeight(realHei());
        this.canvas.setWidth(this.getStage().getWidth());
    }

    @Override
    public void onRequestClose(WindowEvent event) {
        super.onRequestClose(event);
        this.fProxy.stop();
        MainViewController.newInstance().openWindow();
    }

    /**
     * 计算画布能够伸缩最大高度
     */
    private double realHei() {
        var parent = getParent();
        var a = parent.getHeight();
        var c = this.topBox.getHeight();
        return a - c;
    }

}
