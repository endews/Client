package io.github.solclient.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;

import org.lwjgl.opengl.GL11;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;

import io.github.solclient.client.Client;
import io.github.solclient.client.mod.impl.SolClientMod;
import io.github.solclient.client.util.data.Colour;
import io.github.solclient.client.util.data.Rectangle;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.Util.EnumOS;

@UtilityClass
public class Utils {

	private PrintStream out;
	public final ExecutorService MAIN_EXECUTOR = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
	public final Comparator<String> STRING_WIDTH_COMPARATOR = Comparator.comparingInt(Utils::getStringWidth);

	static {
		try {
			out = new PrintStream(System.out, true, "UTF-8");
		}
		catch(UnsupportedEncodingException error) {
			out = System.out;
		}
	}

	private int getStringWidth(String text) {
		return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
	}

	public void drawHorizontalLine(double startX, double endX, double y, int colour) {
		if(endX < startX) {
			double swap = startX;
			startX = endX;
			endX = swap;
		}

		drawRectangle(startX, y, endX + 1, y + 1, colour);
	}

	public void drawVerticalLine(double x, double startY, double endY, int colour) {
		if(endY < startY) {
			double swap = startY;
			startY = endY;
			endY = swap;
		}

		drawRectangle(x, startY + 1, x + 1, endY, colour);
	}

	public void drawOutline(Rectangle rectangle, Colour colour) {
		drawOutline(rectangle.getX(), rectangle.getY(), rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight(),
				colour.getValue());
	}

	public void drawOutline(double left, double top, double right, double bottom, int colour) {
		drawHorizontalLine(left, right - 1, top, colour);
		drawHorizontalLine(left, right - 1, bottom -1, colour);
		drawVerticalLine(left, top, bottom - 1, colour);
		drawVerticalLine(right - 1, top, bottom - 1, colour);
	}

	public void drawRectangle(double x, double y, double right, double bottom, int colour) {
		if(x < right) {
            double swap = x;
            x = right;
            right = swap;
        }

        if(y < bottom) {
        	double swap = y;
            y = bottom;
            bottom = swap;
        }

		float r = (colour >> 24 & 255) / 255.0F;
		float g = (colour >> 16 & 255) / 255.0F;
		float b = (colour >> 8 & 255) / 255.0F;
		float a = (colour & 255) / 255.0F;

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(g, b, a, r);

		worldrenderer.begin(7, DefaultVertexFormats.POSITION);
		worldrenderer.pos(x, bottom, 0.0D).endVertex();
		worldrenderer.pos(right, bottom, 0.0D).endVertex();
		worldrenderer.pos(right, y, 0.0D).endVertex();
		worldrenderer.pos(x, y, 0.0D).endVertex();
		tessellator.draw();

		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	public void drawRectangle(Rectangle rectangle, Colour colour) {
		GuiScreen.drawRect(rectangle.getX(), rectangle.getY(), rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight(),
				colour.getValue());
	}

	public long toMegabytes(long bytes) {
		return bytes / 1024L / 1024L;
	}

	public int lerpColour(int start, int end, float percent) {
		if(percent >= 1) {
			return end;
		}

		Colour startColour = new Colour(start);
		Colour endColour = new Colour(end);

		if(startColour.getAlpha() == 0) {
			startColour = endColour.withAlpha(0);
		}
		else if(endColour.getAlpha() == 0) {
			endColour = startColour.withAlpha(0);
		}

		return new Colour(
				lerp(startColour.getRed(), endColour.getRed(), percent),
				lerp(startColour.getGreen(), endColour.getGreen(), percent),
				lerp(startColour.getBlue(), endColour.getBlue(), percent),
				lerp(startColour.getAlpha(), endColour.getAlpha(), percent)
		).getValue();
	}

	public int lerp(int start, int end, float percent) {
		return Math.round(start + ((end - start) * percent));
	}

	public void scissor(Rectangle rectangle) {
		scissor(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
	}

	public void scissor(double x, double y, double width, double height) {
		ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
		double scale = resolution.getScaleFactor();

		GL11.glScissor((int) (x * scale),
				(int) ((resolution.getScaledHeight() - height - y) * scale),
				(int) (width * scale), (int) (height * scale));
	}

	public void playClickSound(boolean ui) {
		if(ui && !SolClientMod.instance.buttonClicks) {
			return;
		}

		Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
	}

	@SneakyThrows
	public URL sneakyParse(String url) {
		return new URL(url);
	}

	/*
	 * Single following method:
	 *
	 *       Copyright (C) 2018-present Hyperium <https://hyperium.cc/>
	 *
	 *       This program is free software: you can redistribute it and/or modify
	 *       it under the terms of the GNU Lesser General Public License as published
	 *       by the Free Software Foundation, either version 3 of the License, or
	 *       (at your option) any later version.
	 *
	 *       This program is distributed in the hope that it will be useful,
	 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
	 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 *       GNU Lesser General Public License for more details.
	 *
	 *       You should have received a copy of the GNU Lesser General Public License
	 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
	 */
	public void drawCircle(float xx, float yy, int radius, int col) {
		float f = (col >> 24 & 0xFF) / 255.0F;
		float f2 = (col >> 16 & 0xFF) / 255.0F;
		float f3 = (col >> 8 & 0xFF) / 255.0F;
		float f4 = (col & 0xFF) / 255.0F;
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glBegin(2);
		GlStateManager.color(f2, f3, f4, f);

		for (int i = 0; i < 70; i++) {
			float x = radius * MathHelper.cos((float) (i * 0.08975979010256552D));
			float y = radius * MathHelper.sin((float) (i * 0.08975979010256552D));
			GL11.glVertex2f(xx + x, yy + y);
		}

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnd();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glPopMatrix();
	}

	public GuiChat getChatGui() {
		GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
		if(currentScreen != null && currentScreen instanceof GuiChat) {
			return (GuiChat) currentScreen;
		}
		return null;
	}

	public void drawTexture(int x, int y, int textureX, int textureY, int width, int height, int zLevel) {
		float xMultiplier = 0.00390625F;
		float yMultiplier = 0.00390625F;
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
		worldrenderer.pos(x, y + height, zLevel).tex(((textureX) * xMultiplier),
				(textureY + height) * yMultiplier).endVertex();
		worldrenderer.pos(x + width, y + height, zLevel).tex((textureX + width) * xMultiplier, (textureY + height) * yMultiplier).endVertex();
		worldrenderer.pos(x + width, y + 0, zLevel).tex((textureX + width) * xMultiplier, (textureY + 0) * yMultiplier).endVertex();
		worldrenderer.pos(x, y, zLevel).tex(((textureX) * xMultiplier),
				((textureY + 0) * yMultiplier)).endVertex();
		tessellator.draw();
	}

	public void drawGradientRect(int left, int top, int right, int bottom, int startColour, int endColour) {
		float alpha1 = (startColour >> 24 & 255) / 255.0F;
		float red1 = (startColour >> 16 & 255) / 255.0F;
		float green1 = (startColour >> 8 & 255) / 255.0F;
		float blue1 = (startColour & 255) / 255.0F;
		float alpha2 = (endColour >> 24 & 255) / 255.0F;
		float red2 = (endColour >> 16 & 255) / 255.0F;
		float green2 = (endColour >> 8 & 255) / 255.0F;
		float blue2 = (endColour & 255) / 255.0F;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.shadeModel(7425);
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		worldrenderer.pos(right, top, 0).color(red1, green1, blue1, alpha1).endVertex();
		worldrenderer.pos(left, top, 0).color(red1, green1, blue1, alpha1).endVertex();
		worldrenderer.pos(left, bottom, 0).color(red2, green2, blue2, alpha2).endVertex();
		worldrenderer.pos(right, bottom, 0).color(red2, green2, blue2, alpha2).endVertex();
		tessellator.draw();
		GlStateManager.shadeModel(7424);
		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
	}

	public boolean isSpectatingEntityInReplay() {
		return ReplayModReplay.instance.getReplayHandler() != null
				&& !(Minecraft.getMinecraft().getRenderViewEntity() instanceof CameraEntity);
	}

	public String getTextureScale() {
		ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());

		if(resolution.getScaleFactor() > 0 && resolution.getScaleFactor() < 5) {
			return resolution.getScaleFactor() + "x";
		}

		return "4x";
	}

	public String urlToString(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.addRequestProperty("User-Agent", System.getProperty("http.agent")); // Force consistent behaviour

		InputStream in = connection.getInputStream();

		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder result = new StringBuilder();

		String line;
		while((line = reader.readLine()) != null) {
			result.append(line).append("\n");
		}

		return result.toString();
	}

	public int getShadowColour(int value) {
		return (value & 16579836) >> 2 | value & -16777216;
	}

	public void pingServer(String address, IntConsumer callback) throws UnknownHostException {
		ServerAddress serverAddress = ServerAddress.fromString(address);
		NetworkManager networkManager = NetworkManager.createNetworkManagerAndConnect(
				InetAddress.getByName(serverAddress.getIP()), serverAddress.getPort(), false);

		networkManager.setNetHandler(new INetHandlerStatusClient() {

			private boolean expected = false;
			private long time = 0L;

			@Override
			public void handleServerInfo(S00PacketServerInfo packetIn) {
				if(expected) {
					networkManager.closeChannel(new ChatComponentText("Received unrequested status"));
				}
				else {
					expected = true;
					time = Minecraft.getSystemTime();
					networkManager.sendPacket(new C01PacketPing(this.time));
				}
			}

			@Override
			public void handlePong(S01PacketPong packetIn) {
				long systemTime = Minecraft.getSystemTime();
				callback.accept((int) (systemTime - time));
				networkManager.closeChannel(new ChatComponentText("Finished"));
			}

			@Override
			public void onDisconnect(IChatComponent reason) {
				callback.accept(-1);
			}
		});

		networkManager.sendPacket(
				new C00Handshake(47, serverAddress.getIP(), serverAddress.getPort(), EnumConnectionState.STATUS));
		networkManager.sendPacket(new C00PacketServerQuery());
	}

	public int randomInt(int from, int to) {
		return ThreadLocalRandom.current().nextInt(from, to + 1); // https://stackoverflow.com/a/363692
	}

	public void openUrl(String url) {
		sendLauncherMessage("openUrl", url);
	}

	private void sendLauncherMessage(String type, String... arguments) {
		out.println("message " + System.getProperty("io.github.solclient.client.secret") + " " + type + " " + String.join(" ", arguments));
	}

	public String getRelativeToPackFolder(File packFile) {
		String relative = new File(Minecraft.getMinecraft().mcDataDir, "resourcepacks").toPath().toAbsolutePath()
				.relativize(packFile.toPath().toAbsolutePath()).toString();

		if(Util.getOSType() == EnumOS.WINDOWS) {
			relative = relative.replace("\\", "/"); // Just to be safe
		}

		return relative;
	}

	public void resetLineWidth() {
		// Reset the fishing rod line back to its normal width.
		// Fun fact: the line should actually be thinner, but it's overriden by the
		// block selection.
		GL11.glLineWidth(2);
	}

	public void drawFloatRectangle(float left, float top, float right, float bottom, int colour) {
		if(left < right) {
			float swap = left;
			left = right;
			right = swap;
		}

		if(top < bottom) {
			float swap = top;
			top = bottom;
			bottom = swap;
		}

		float f3 = (colour >> 24 & 255) / 255.0F;
		float f = (colour >> 16 & 255) / 255.0F;
		float f1 = (colour >> 8 & 255) / 255.0F;
		float f2 = (colour & 255) / 255.0F;
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(f, f1, f2, f3);
		worldrenderer.begin(7, DefaultVertexFormats.POSITION);
		worldrenderer.pos(left, bottom, 0.0D).endVertex();
		worldrenderer.pos(right, bottom, 0.0D).endVertex();
		worldrenderer.pos(right, top, 0.0D).endVertex();
		worldrenderer.pos(left, top, 0.0D).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	public static String getScoreboardTitle() {
		Minecraft mc = Minecraft.getMinecraft();

		if(mc.theWorld != null && mc.theWorld.getScoreboard() != null) {
			ScoreObjective first = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);

			if(first != null) {
				return EnumChatFormatting.getTextWithoutFormattingCodes(first.getDisplayName());
			}
		}

		return null;
	}

	public static String getNativeFileExtension() {
		switch(Util.getOSType()) {
			case WINDOWS:
				return "dll";
			case OSX:
				return "dylib";
			default:
				return "so";
		}
	}

	public double max(double[] doubles) {
		double max = 0;

		for(double d : doubles) {
			if(max < d) {
				max = d;
			}
		}

		return max;
	}

	public void fillBox(AxisAlignedBB box) {
		GlStateManager.disableCull();
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();

		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		tessellator.draw();

		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
		tessellator.draw();

		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		tessellator.draw();

		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		tessellator.draw();

		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
		tessellator.draw();

		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		tessellator.draw();

		GlStateManager.enableCull();
	}

	public static void earlyLoad(String name) {
		try {
			Class.forName(name);
		}
		catch(Exception error) {
			Client.LOGGER.error("Could not early load " + name + ". This may cause further issues.");
		}
	}

}
