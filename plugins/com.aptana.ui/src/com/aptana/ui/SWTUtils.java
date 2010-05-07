package com.aptana.ui;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

public class SWTUtils
{

	private static final String SMALL_FONT = "com.aptana.ui.small_font"; //$NON-NLS-1$

	private static Color errorColor;

	static
	{
		ColorRegistry cm = JFaceResources.getColorRegistry();
		RGB errorRGB = new RGB(255, 255, 180);
		cm.put("error", errorRGB); //$NON-NLS-1$
		errorColor = cm.get("error"); //$NON-NLS-1$
	}

	/**
	 * Centers the shell on screen, and re-packs it to the preferred size. Packing is necessary as otherwise dialogs
	 * tend to get cut off on the Mac
	 * 
	 * @param shell
	 *            The shell to center
	 * @param parent
	 *            The shell to center within
	 */
	public static void centerAndPack(Shell shell, Shell parent)
	{
		center(shell, parent);
		shell.pack();
	}

	/**
	 * Centers the shell on screen.
	 * 
	 * @param shell
	 *            The shell to center
	 * @param parent
	 *            The shell to center within
	 */
	public static void center(Shell shell, Shell parent)
	{
		Rectangle parentSize = parent.getBounds();
		Rectangle mySize = shell.getBounds();

		int locationX, locationY;
		locationX = (parentSize.width - mySize.width) / 2 + parentSize.x;
		locationY = (parentSize.height - mySize.height) / 2 + parentSize.y;
		shell.setLocation(new Point(locationX, locationY));
	}

	/**
	 * Gets the default small font from the JFace font registry.
	 * 
	 * @return default small font
	 */
	public static Font getDefaultSmallFont()
	{
		Font small = JFaceResources.getFontRegistry().get(SMALL_FONT);
		if (small != null)
		{
			return small;
		}

		Font f = JFaceResources.getDefaultFont();
		FontData[] smaller = resizeFont(f, -2);
		JFaceResources.getFontRegistry().put(SMALL_FONT, smaller);
		return JFaceResources.getFontRegistry().get(SMALL_FONT);
	}

	/**
	 * Finds and caches the iamge from the image descriptor for this particular plugin.
	 * 
	 * @param plugin
	 *            the plugin to search
	 * @param path
	 *            the path to the image
	 * @return the image, or null if not found
	 */
	public static Image getImage(AbstractUIPlugin plugin, String path)
	{
		return getImage(plugin.getBundle(), path);
	}

	/**
	 * Finds and caches the image from the image descriptor for this particular bundle.
	 * 
	 * @param bundle
	 *            the bundle to search
	 * @param path
	 *            the path to the image
	 * @return the image, or null if not found
	 */
	public static Image getImage(Bundle bundle, String path)
	{
		if (path.charAt(0) != '/')
		{
			path = "/" + path; //$NON-NLS-1$
		}

		String computedName = bundle.getSymbolicName() + path;
		Image image = JFaceResources.getImage(computedName);
		if (image != null)
		{
			return image;
		}

		ImageDescriptor id = AbstractUIPlugin.imageDescriptorFromPlugin(bundle.getSymbolicName(), path);
		if (id != null)
		{
			JFaceResources.getImageRegistry().put(computedName, id);
			return JFaceResources.getImage(computedName);
		}
		return null;
	}

	/**
	 * Returns a version of the specified font, resized by the requested size.
	 * 
	 * @param font
	 *            the font to resize
	 * @param size
	 *            the font size
	 * @return resized font data
	 */
	public static FontData[] resizeFont(Font font, int size)
	{
		FontData[] datas = font.getFontData();
		for (FontData data : datas)
		{
			data.setHeight(data.getHeight() + size);
		}

		return datas;
	}

	/**
	 * Bolds a font.
	 * 
	 * @param font
	 * @return bolded font data
	 */
	public static FontData[] boldFont(Font font)
	{
		FontData[] datas = font.getFontData();
		for (FontData data : datas)
		{
			data.setStyle(data.getStyle() | SWT.BOLD);
		}
		return datas;
	}

	/**
	 * Tests if the widget value is empty. If so, it adds an error color to the background of the cell.
	 * 
	 * @param widget
	 *            the widget to set text for
	 * @param validSelectionIndex
	 *            the first item that is a "valid" selection
	 * @return boolean
	 */
	public static boolean testWidgetValue(Combo widget, int validSelectionIndex)
	{
		final int selectionIndex;
		if (validSelectionIndex > 0)
		{
			selectionIndex = validSelectionIndex;
		}
		else
		{
			selectionIndex = 0;
		}

		String text = widget.getText();
		if (text == null || text.length() == 0 || widget.getSelectionIndex() < selectionIndex)
		{
			widget.setBackground(errorColor);
			final ModifyListener ml = new ModifyListener()
			{

				public void modifyText(ModifyEvent e)
				{
					Combo c = (Combo) e.widget;
					String t = c.getText();
					if (t != null && t.length() > 0 || c.getSelectionIndex() >= selectionIndex)
					{
						c.setBackground(null);
					}
					else
					{
						c.setBackground(errorColor);
					}
				}
			};
			widget.addModifyListener(ml);
			return false;
		}
		return true;
	}
}