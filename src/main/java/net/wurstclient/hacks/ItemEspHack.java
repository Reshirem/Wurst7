/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"item esp", "ItemTracers", "item tracers"})
public final class ItemEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final CheckboxSetting names =
		new CheckboxSetting("Show item names", true);
	private final EnumSetting<Style> style =
		new EnumSetting<>("Style", Style.values(), Style.BOXES);
	
	private int itemBox;
	private final ArrayList<ItemEntity> items = new ArrayList<>();
	
	public ItemEspHack()
	{
		super("ItemESP", "Highlights nearby items.");
		setCategory(Category.RENDER);
		
		if(names != null)
			addSetting(names);
		addSetting(style);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getEventManager().add(UpdateListener.class, this);
		WURST.getEventManager().add(CameraTransformViewBobbingListener.class,
			this);
		WURST.getEventManager().add(RenderListener.class, this);
	}
	
	private void createItemBoxDisplayList()
	{
		itemBox = GL11.glGenLists(1);
		GL11.glNewList(itemBox, GL11.GL_COMPILE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glColor4f(1, 1, 0, 0.5F);
		RenderUtils
			.drawOutlinedBox(new Box(-0.175, 0, -0.175, 0.175, 0.35, 0.175));
		GL11.glEndList();
	}
	
	@Override
	public void onDisable()
	{
		WURST.getEventManager().remove(UpdateListener.class, this);
		WURST.getEventManager().remove(CameraTransformViewBobbingListener.class,
			this);
		WURST.getEventManager().remove(RenderListener.class, this);
		
		GL11.glDeleteLists(itemBox, 1);
		itemBox = 0;
	}
	
	@Override
	public void onUpdate()
	{
		if(itemBox == 0)
			createItemBoxDisplayList();
		
		items.clear();
		for(Entity entity : MC.world.getEntities())
			if(entity instanceof ItemEntity)
				items.add((ItemEntity)entity);
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.getSelected().lines)
			event.cancel();
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		// GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		renderBoxes(partialTicks);
		
		if(style.getSelected().lines)
			renderTracers(partialTicks);
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void renderBoxes(double partialTicks)
	{
		for(ItemEntity e : items)
		{
			GL11.glPushMatrix();
			GL11.glTranslated(e.prevX + (e.x - e.prevX) * partialTicks,
				e.prevY + (e.y - e.prevY) * partialTicks,
				e.prevZ + (e.z - e.prevZ) * partialTicks);
			
			if(style.getSelected().boxes)
				GL11.glCallList(itemBox);
			
			if(names != null && names.isChecked())
			{
				ItemStack stack = e.getStack();
				GameRenderer.renderFloatingText(MC.textRenderer,
					stack.getCount() + "x "
						+ stack.getName().asFormattedString(),
					0, 1, 0, 0, MC.getEntityRenderManager().cameraYaw,
					MC.getEntityRenderManager().cameraPitch, false);
				GL11.glDisable(GL11.GL_LIGHTING);
			}
			
			GL11.glPopMatrix();
		}
	}
	
	private void renderTracers(double partialTicks)
	{
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glColor4f(1, 1, 0, 0.5F);
		
		Vec3d start = RotationUtils.getClientLookVec()
			.add(0, MC.player.getEyeHeight(MC.player.getPose()), 0)
			.add(BlockEntityRenderDispatcher.renderOffsetX,
				BlockEntityRenderDispatcher.renderOffsetY,
				BlockEntityRenderDispatcher.renderOffsetZ);
		
		GL11.glBegin(GL11.GL_LINES);
		for(ItemEntity e : items)
		{
			Vec3d end = e.getBoundingBox().getCenter().subtract(
				new Vec3d(e.x, e.y, e.z).subtract(e.prevX, e.prevY, e.prevZ)
					.multiply(1 - partialTicks));
			
			GL11.glVertex3d(start.x, start.y, start.z);
			GL11.glVertex3d(end.x, end.y, end.z);
		}
		GL11.glEnd();
	}
	
	private enum Style
	{
		BOXES("Boxes only", true, false),
		LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);
		
		private final String name;
		private final boolean boxes;
		private final boolean lines;
		
		private Style(String name, boolean boxes, boolean lines)
		{
			this.name = name;
			this.boxes = boxes;
			this.lines = lines;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}