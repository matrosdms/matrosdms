package net.schwehla.matrosdms.rcp.swt.popupshell.widgets.notifications;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import net.schwehla.matrosdms.rcp.swt.listener.MoveShellListener;
import net.schwehla.matrosdms.rcp.swt.popupshell.effects.FadeEffect;
import net.schwehla.matrosdms.rcp.swt.popupshell.effects.FadeEffect.Fadeable;
import net.schwehla.matrosdms.rcp.swt.popupshell.effects.InvalidEffectArgumentException;
import net.schwehla.matrosdms.rcp.swt.popupshell.helpers.AncestryHelper;
import net.schwehla.matrosdms.rcp.swt.popupshell.helpers.WidgetHelper;
import net.schwehla.matrosdms.rcp.swt.popupshell.util.ColorFactory;
import net.schwehla.matrosdms.rcp.swt.popupshell.util.DisplaySafe;
import net.schwehla.matrosdms.rcp.swt.popupshell.widgets.CustomElementDataProvider;

/**
 * PopOverShell provides a simple interface for popping a Shell on top of any Object that subclasses
 * <code>Control</code> or implements <code>CustomElementDataProvider</code>
 */
public abstract class PopOverShell extends Widget implements Fadeable {
  private static final Logger LOG = Logger.getLogger(PopOverShell.class.getName());

  private static final RGB BACKGROUND_COLOR = new RGB(74, 74, 74);
  private static final int FADE_OUT_TIME = 200; //milliseconds
  private static final int FULLY_VISIBLE_ALPHA = 255; //fully opaque
  private static final int FULLY_HIDDEN_ALPHA = 0; //fully transparent

  static final VerticalLocation DEFAULT_DISPLAY_LOCATION = VerticalLocation.BELOW;
  static final CenteringEdge DEFAULT_EDGE_CENTERED = CenteringEdge.RIGHT_DIRECT;

  VerticalLocation popOverAboveOrBelowParent = DEFAULT_DISPLAY_LOCATION;
  CenteringEdge popOverEdgeCenteredOnParent = DEFAULT_EDGE_CENTERED;

  private Object fadeLock = new Object();

  protected final Control parentControl;
  protected Shell popOverShell;

  private Shell parentShell;
  private PoppedOverItem poppedOverItem;
  private Listener popOverListener;
  private Listener parentListener;

  private Color backgroundColor;

  private Region popOverRegion;

  private boolean positionRelativeParent = false;
  private boolean fadeEffectInProgress = false;

  private DisplaySafe displaySafe;

  /**
   * Provides the backbone for Custom Widgets that need a <code>Shell</code> popped over a <code>Control</code> or
   * <code>CustomElementDataProvider</code>. If you're using a <code>CustomElementDataProvider</code>, pass the
   * <code>CustomElementDataProvider.getPaintedElement()</code> as the parentControl.
   * @param parentControl The control you want the PopOverShell to appear above. In the case of
   *                      <code>CustomElementDataProvider</code>, pass
   *                      <code>CustomElementDataProvider.getPaintedElement()</code>.
   * @param customElementDataProvider The <code>CustomElementDataProvider</code> you want the PopOverShell to appear
   *                                  above (or null if you're using a Control)
   */
  public PopOverShell(Control parentControl, CustomElementDataProvider customElementDataProvider) {
    super(parentControl, SWT.NONE);

    displaySafe = new DisplaySafe();

    if (customElementDataProvider != null) {
      poppedOverItem = new PoppedOverItem(customElementDataProvider);
    } else {
      poppedOverItem = new PoppedOverItem(parentControl);
    }

    this.parentControl = parentControl;
    parentShell = AncestryHelper.getShellFromControl(poppedOverItem.getControl());

    backgroundColor = ColorFactory.getColor(getDisplay(), BACKGROUND_COLOR);

    // SWT.TOOL adds a drop shadow on supported platforms
    popOverShell = new Shell(parentShell, SWT.RESIZE | SWT.BORDER  | SWT.ON_TOP | SWT.TITLE & ~SWT.CLOSE );

    
    
    popOverShell.setBackground(backgroundColor);
    popOverShell.setLayout(new FillLayout());
    
    

    attachListeners();
    
    
  
  }
  
  Point oldSize ;
  Point oldLocation ;

/**
   * Shows the PopOverShell in a suitable location relative to the parent component. Classes extending PopOverShell will
   * provide the <code>Region</code> via the abstract <code>getAppropriatePopOverRegion()</code> method.
   */
  public void show() {
    runBeforeShowPopOverShell();

    Point popOverShellSize = getAppropriatePopOverSize();

    
    if (oldSize != null) {
    	popOverShell.setSize(oldSize);
    } else {
    	
        popOverRegion = new Region();
        popOverRegion.add(new Rectangle(0, 0, popOverShellSize.x, popOverShellSize.y));

        popOverShell.setRegion(popOverRegion);
        
    	popOverShell.setSize(popOverRegion.getBounds().width, popOverRegion.getBounds().height);
    }
    
    if (oldLocation != null) {
    	popOverShell.setLocation(oldLocation);
    } else {
        Point location = getPopOverShellLocation(parentShell, poppedOverItem, popOverRegion);
        
        popOverShell.setLocation(location);
    }
    

    popOverShell.setAlpha(FULLY_VISIBLE_ALPHA);
    popOverShell.setVisible(true);
    

    
  }

  /**
   * Toggles visibility of the PopOverShell. If the PopOverShell is visible, it will fade it from the screen, otherwise
   * it will pop it up.
   */
  public void toggle() {
    if (isVisible() && !getIsFadeEffectInProgress()) {
      fadeOut();
      
      oldSize = popOverShell.getSize();
      oldLocation = popOverShell.getLocation();
    } else {
      show();
    }
  }
  
  
  public void relocate() {
	  
		if (isDisposed() || !isVisible()) {
			return;
		}
		
	    Point location = getPopOverShellLocation(parentShell, poppedOverItem, popOverRegion);
		popOverShell.setLocation(location);
	}
  

  public PopOverShell setPositionRelativeParent(boolean positionRelativeParent) {
    this.positionRelativeParent = positionRelativeParent;
    return this;
  }

  /**
   * Implementers of this method return a Point describing the width and height the PopOverShell should be.
   * @return A Point object describing the appropriate PopOverSize. The x is the width and y is the height.
   */
  abstract Point getAppropriatePopOverSize();

  /**
   * Implementers of this method run any logic that needs to be executed before the PopOverShell is shown to
   * the user.
   */
  abstract void runBeforeShowPopOverShell();

  /**
   * Implementers of this method should do any clean-up needed to reset the widget to its default state.
   */
  abstract void resetWidget();

  /**
   * Called when the parent <code>PopOverShell</code> is disposed. Make sure you clean up any leftover elements
   * that need to be disposed. See https://github.com/ReadyTalk/swt-bling/wiki/Finding-SWT-Resource-Leaks-with-Sleak
   * for more information on detecting leaks with Sleak.
   */
  abstract void widgetDispose();

  PoppedOverItem getPoppedOverItem() {
    return poppedOverItem;
  }
  Shell getPopOverShell() { return popOverShell; }

  public void checkSubclass() {
    //no-op
  }

  private void attachListeners() {
    popOverListener = new Listener() {
      public void handleEvent(Event event) {
        switch (event.type) {
          case SWT.Dispose:
            onDispose(event);
            break;
        }
      }
    };

    addListener(SWT.Dispose, popOverListener);

    parentListener = new Listener() {
      public void handleEvent(Event event) {
        dispose();
      }
    };
    parentControl.addListener(SWT.Dispose, parentListener);
    
    
    MoveShellListener moveListener = new MoveShellListener(popOverShell);
    parentControl.addListener(SWT.Dispose, moveListener);
    
  }

  private void onDispose(Event event) {
    widgetDispose();

    parentControl.removeListener(SWT.Dispose, parentListener);
    removeListener(SWT.Dispose, parentListener);
    event.type = SWT.None;

    popOverShell.dispose();
    popOverShell = null;

    if (popOverRegion != null) {
      popOverRegion.dispose();
    }
    popOverRegion = null;
  }

  private Point getPopOverShellLocation(Shell parentShell, PoppedOverItem poppedOverItem, Region popOverRegion) {

    Point location;
    Rectangle displayBounds = null;

    try {
      Display display = displaySafe.getLatestDisplay();
      displayBounds = display.getBounds();
    } catch (DisplaySafe.NullDisplayException nde) {
      LOG.warning("Could not find display");
    }

    Rectangle popOverBounds = popOverRegion.getBounds();
    Point poppedOverItemLocationRelativeToDisplay =
            getPoppedOverItemRelativeLocation(poppedOverItem);

    // Guess on the location first
    location = getPopOverDisplayPoint(popOverBounds, poppedOverItem, poppedOverItemLocationRelativeToDisplay,
            popOverEdgeCenteredOnParent, popOverAboveOrBelowParent);

    // Adjust as needed
    if (popOverAboveOrBelowParent == VerticalLocation.BELOW) {
      if (isBottomCutOff(displayBounds, location, popOverBounds)) {
        popOverAboveOrBelowParent = VerticalLocation.ABOVE;
        location.y = getPopOverYLocation(popOverBounds, poppedOverItem, poppedOverItemLocationRelativeToDisplay,
                popOverAboveOrBelowParent);
      }
    } else {
      if (isTopCutOff(location)) {
        popOverAboveOrBelowParent = VerticalLocation.BELOW;
        location.y = getPopOverYLocation(popOverBounds, poppedOverItem, poppedOverItemLocationRelativeToDisplay,
            popOverAboveOrBelowParent);
      }
    }

    if (popOverEdgeCenteredOnParent == CenteringEdge.LEFT) {
      if (isRightCutOff(displayBounds, location, popOverBounds)) {
        popOverEdgeCenteredOnParent = CenteringEdge.RIGHT;
        location.x = getPopOverXLocation(popOverBounds, poppedOverItem, poppedOverItemLocationRelativeToDisplay,
                popOverEdgeCenteredOnParent);
      }
    } else {
      if (isLeftCutOff(location)) {
        popOverEdgeCenteredOnParent = CenteringEdge.LEFT;
        location.x = getPopOverXLocation(popOverBounds, poppedOverItem, poppedOverItemLocationRelativeToDisplay,
            popOverEdgeCenteredOnParent);
      }
    }

    if (isStillOffScreen(displayBounds, location, popOverBounds)) {
      location = getPopOverLocationControlOffscreen(displayBounds, popOverRegion,
              poppedOverItemLocationRelativeToDisplay, location);
    }

    return location;
  }

  boolean isBottomCutOff(Rectangle displayBounds, Point locationRelativeToDisplay,
                                              Rectangle popOverBounds) {
    boolean isBottomCutOff = false;
    int lowestYPosition = locationRelativeToDisplay.y + popOverBounds.height;

    if (displayBounds != null && !displayBounds.contains(new Point(0, lowestYPosition))) {
      isBottomCutOff = true;
    }

    return isBottomCutOff;
  }

  boolean isTopCutOff(Point locationRelativeToDisplay) {
    return locationRelativeToDisplay.y >= 0 ? false : true;
  }

  boolean isRightCutOff(Rectangle displayBounds, Point locationRelativeToDisplay,
                                                     Rectangle popOverBounds) {
    boolean isRightCutOff = false;
    int farthestXPosition = locationRelativeToDisplay.x + popOverBounds.width;

    if (displayBounds != null && !displayBounds.contains(new Point(farthestXPosition, 0))) {
      popOverEdgeCenteredOnParent = CenteringEdge.RIGHT;
      isRightCutOff = true;
    }

    return isRightCutOff;
  }

  boolean isLeftCutOff(Point locationRelativeToDisplay) {
    return locationRelativeToDisplay.x >= 0 ? false : true;
  }

  boolean isStillOffScreen(Rectangle displayBounds, Point locationRelativeToDisplay,
                           Rectangle popOverBounds) {
    boolean isStillOffScreen = false;
    Point currentPosition = new Point (locationRelativeToDisplay.x + popOverBounds.width,
            locationRelativeToDisplay.y + popOverBounds.height);
    if (!displayBounds.contains(currentPosition)) {
      isStillOffScreen = true;
    }

    return isStillOffScreen;
  }

  Point getPoppedOverItemRelativeLocation(PoppedOverItem poppedOverItem) {
    Point location = null;
    if (positionRelativeParent == false) {
      Display display = null;
      try {
        display = displaySafe.getLatestDisplay();
      } catch (DisplaySafe.NullDisplayException nde) {
        LOG.warning("Could not find active display.");
      }

      if(display != null) {
        location = display.map(parentShell, null, poppedOverItem.getLocation());
      }
    } else {
      location = parentControl.toDisplay(poppedOverItem.getLocation());
    }
    return location;
  }

  private Point getPopOverDisplayPoint(Rectangle popOverBounds,
                                       PoppedOverItem poppedOverItem,
                                       Point poppedOverItemLocationRelativeToDisplay,
                                       CenteringEdge popOverCornerCenteredOnParent,
                                       VerticalLocation popOverAboveOrBelowParent) {
    Point location = new Point(0, 0);
    location.x = getPopOverXLocation(popOverBounds, poppedOverItem, poppedOverItemLocationRelativeToDisplay,
            popOverCornerCenteredOnParent);
    location.y = getPopOverYLocation(popOverBounds, poppedOverItem, poppedOverItemLocationRelativeToDisplay,
            popOverAboveOrBelowParent);
    return location;
  }

  private int getPopOverXLocation(Rectangle popOverBounds,
                                  PoppedOverItem poppedOverItem,
                                  Point poppedOverItemLocationRelativeToDisplay,
                                  CenteringEdge popOverCornerCenteredOnParent) {
    int popOverX = 0;
    switch(popOverCornerCenteredOnParent) {
    
      case RIGHT_DIRECT:
    	popOverX = poppedOverItemLocationRelativeToDisplay.x ;
    	break;
      case LEFT:
        popOverX = poppedOverItemLocationRelativeToDisplay.x + (poppedOverItem.getSize().x / 2);
        break;
      case RIGHT:
        popOverX = poppedOverItemLocationRelativeToDisplay.x - popOverBounds.width + (poppedOverItem.getSize().x / 2);
        break;
    }

    return popOverX;
  }

  private int getPopOverYLocation(Rectangle popOverBounds,
                                  PoppedOverItem poppedOverItem,
                                  Point poppedOverItemLocationRelativeToDisplay,
                                  VerticalLocation aboveOrBelow) {
    int popOverY = 0;
    switch (aboveOrBelow) {
      case ABOVE:
        popOverY = poppedOverItemLocationRelativeToDisplay.y - popOverBounds.height;
        break;
      case BELOW:
        popOverY = poppedOverItemLocationRelativeToDisplay.y + poppedOverItem.getSize().y;
        break;
    }

    return popOverY;
  }

  private Point getPopOverLocationControlOffscreen(Rectangle displayBounds,
                                                   Region popOverRegion,
                                                   Point poppedOverItemLocationRelativeToDisplay,
                                                   Point popOverOffscreenLocation) {
    Point appropriateDisplayLocation = popOverOffscreenLocation;
    Rectangle popOverRegionBounds = popOverRegion.getBounds();
    if (!displayBounds.contains(new Point(poppedOverItemLocationRelativeToDisplay.x + popOverRegionBounds.width, 0))) {
      appropriateDisplayLocation.x = displayBounds.width - popOverRegionBounds.width;
    }
    if (!displayBounds.contains(new Point(0, poppedOverItemLocationRelativeToDisplay.y + popOverRegionBounds.height))) {
      appropriateDisplayLocation.y = displayBounds.height - popOverRegionBounds.height;
    }

    return appropriateDisplayLocation;
  }

  /**
   * Returns whether the PopOverShell is currently visible on screen.
   * Note: If you utilize <code>PopOverShell.fadeOut()</code>, this method will return true while it's fading.
   * To determine if it's fading out, call <code>PopOverShell.getIsFadeEffectInProgress</code>
   * @return Visibility state of the PopOverShell
   */
  public boolean isVisible() {
    boolean isVisible = false;
    if (WidgetHelper.isWidgetSafe(popOverShell)){
      isVisible = popOverShell.isVisible();
    }
    return isVisible;
  }

  /**
   * Fades the <code>PopOverShell</code> off the screen.
   */
  public void fadeOut() {
    if (fadeEffectInProgress) {
      return;
    }

    try {
      fadeEffectInProgress = true;
      FadeEffect fade = new FadeEffect.FadeEffectBuilder().
              setFadeable(this).
              setFadeCallback(new PopOverShellFadeCallback()).
              setFadeTimeInMilliseconds(FADE_OUT_TIME).
              setCurrentAlpha(FULLY_VISIBLE_ALPHA).
              setTargetAlpha(FULLY_HIDDEN_ALPHA).build();

      fade.startEffect();
    } catch (InvalidEffectArgumentException e) {
      LOG.warning("Invalid argument provided to FadeEffect.");
    }
  }

  /**
   * Returns whether the PopOverShell is currently fading from the screen.
   * Calls to <code>PopOverShell.isVisible()</code> will return true while the PopOverShell is dismissing.
   * @return Whether or not the PopOverShell is currently fading from the screen
   */
  public boolean getIsFadeEffectInProgress() {
    return fadeEffectInProgress;
  }

  /**
   * Implemented as part of Fadeable. <br/>
   * Users should not interact directly invoke this method.
   */
  public boolean fadeComplete(int targetAlpha) {
    synchronized (fadeLock) {
      boolean isFadeComplete = false;
      if (popOverShell == null || popOverShell.isDisposed() || popOverShell.getAlpha() == targetAlpha) {
        isFadeComplete =  true;
      }

      return isFadeComplete;
    }
  }

  /**
   * Implemented as part of Fadeable. <br/>
   * Users should not interact directly invoke this method.
   */
  public void fade(int alpha) {
    synchronized (fadeLock) {
      popOverShell.setAlpha(alpha);
    }
  }

  void hide() {
    popOverShell.setVisible(false);
    resetState();
    resetWidget();
  }

  private void resetState() {
    popOverAboveOrBelowParent = DEFAULT_DISPLAY_LOCATION;
    popOverEdgeCenteredOnParent = DEFAULT_EDGE_CENTERED;
    fadeEffectInProgress = false;
  }

  private class PopOverShellFadeCallback implements FadeEffect.FadeCallback {
    public void fadeComplete() {
      hide();
    }
  }

  /**
   * A convenience structure for PopOverShell. We could be interacting with a <code>Control</code> (or descendant),
   * or we could be interacting with a {@link CustomElementDataProvider}. This wrapper helps to provide some
   * abstraction.
   */
  public class PoppedOverItem {
    private Control control;
    private CustomElementDataProvider customElementDataProvider;

    public PoppedOverItem(Control control) {
      this.control = control;
    }

    public PoppedOverItem(CustomElementDataProvider customElementDataProvider) {
      this.customElementDataProvider = customElementDataProvider;
    }

    Point getSize() {
      if (control != null) {
        return control.getSize();
      } else {
        return customElementDataProvider.getSize();
      }
    }

    Point getLocation() {
      if (control != null) {
        return control.getLocation();
      } else {
        return customElementDataProvider.getLocation();
      }
    }

    Control getControl() {
      if (control != null) {
        return control;
      } else {
        return customElementDataProvider.getPaintedElement();
      }
    }

    Object getControlOrCustomElement() {
      if (customElementDataProvider != null) {
        return customElementDataProvider;
      } else {
        return control;
      }
    }

    CustomElementDataProvider getCustomElementDataProvider() {
      return customElementDataProvider;
    }
  }
}
