/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe.instance_panel;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.stream.Stream;

public class CompoundCellRenderer extends JLabel implements ListCellRenderer<InstanceBean>{
	
	private InstanceBean ec;

	private Color backColor, foreColor;
	
	private Font valueFont, compoundFont, propertyFont, statusFont;

	private Color selectedBackground, evenBackground, unevenBackground, selectedForeground;
	private Color activatedForeground, deactivatedForeground, disableBackground;
	
	private DecimalFormat numberFormat;
	private ImageIcon loadingGif;

	public CompoundCellRenderer(){
		this.setPreferredSize(new Dimension(210,86));
		initColorsAndFonts();
		this.numberFormat = new DecimalFormat("#0.00");
	}
	
	public void initColorsAndFonts() {
		compoundFont = Fonts.FONT_BOLD.deriveFont(13f);
		propertyFont = Fonts.FONT_BOLD.deriveFont(12f);
		statusFont = Fonts.FONT_BOLD.deriveFont(24f);
		valueFont = Fonts.FONT.deriveFont(12f);

		selectedBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
		selectedForeground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
		evenBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\".background");
		disableBackground = UIManager.getColor("ComboBox.background");
		unevenBackground = new Color(213, 227, 238);
		activatedForeground = UIManager.getColor("List.foreground");
		deactivatedForeground = Color.GRAY;
	}
	
	@Override
	public Component getListCellRendererComponent(
			JList<? extends InstanceBean> list, InstanceBean value,
			int index, boolean isSelected, boolean cellHasFocus) {
		this.ec = value;
		if(isSelected){
			this.backColor = this.selectedBackground;
			this.foreColor = this.selectedForeground;
		}else{
			if(index%2==0) this.backColor = this.evenBackground;
			else this.backColor = this.unevenBackground;
			this.foreColor = this.activatedForeground;
		}
		
		this.setToolTipText(ec.getGUIName());
		
		return this;
	}
	
	
	
	@Override
	public void paint(Graphics g){
		
		Graphics2D g2 = (Graphics2D) g; 
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(this.backColor);
		
		g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getWidth());
		
		FontMetrics compoundFm = g2.getFontMetrics(this.compoundFont);
		FontMetrics propertyFm = g2.getFontMetrics(this.propertyFont);
		FontMetrics valueFm = g2.getFontMetrics(this.valueFont);
		
		g2.setColor(this.foreColor);

		int compoundLength = compoundFm.stringWidth(ec.getGUIName()) + 4;

		boolean trigger = compoundLength + 2 > 198;

		Paint p = g2.getPaint();

		if (trigger) {
			g2.setPaint(new GradientPaint(180, 0, foreColor, 199, 0, backColor));
		}

		g2.drawLine(2, 17, Math.min(197, 2 + compoundLength), 17);

		g2.setFont(compoundFont);
		g2.drawString(ec.getGUIName(), 4, 13);

		if (trigger) g2.setPaint(p);

//		int ms1No = ec.getMs1Spectra().size();
//		int ms2No = ec.getMs2Spectra().size();

		String ionizationProp = "Ionization";
		String focMassProp = "Precursor";
		String rtProp = "RT";
		String confProp = "COSMIC";

		g2.setFont(propertyFont);
		g2.drawString(ionizationProp, 4, 32);
		g2.drawString(focMassProp, 4, 48);
		g2.drawString(rtProp, 4, 64);
		g2.drawString(confProp, 4, 80);

		int xPos = Stream.of(propertyFm.stringWidth(ionizationProp), propertyFm.stringWidth(focMassProp), propertyFm.stringWidth(rtProp))
				.max(Integer::compareTo).get() + 15;

		String ionValue = ec.getIonization().toString();
		double focD = ec.getIonMass();
		String focMass = focD > 0 ? numberFormat.format(focD) + " Da" : "unknown";
		String rtValue = ec.getID().getRt().map(RetentionTime::getRetentionTimeInSeconds).map(s -> s / 60)
				.map(numberFormat::format).map(i -> i + " min").orElse("N/A");

		g2.setFont(valueFont);
		g2.drawString(ionValue, xPos, 32);
		g2.drawString(focMass, xPos, 48);
		g2.drawString(rtValue, xPos, 64);

//		int yPos = 64;
//		int yPos = 80;

		g2.setFont(propertyFont);
		String conf =  ec.getID().getConfidenceScore().map(BigDecimal::valueOf).map(d -> d.setScale(3, RoundingMode.HALF_UP)).map(BigDecimal::toString).orElse("N/A");
		g2.drawString(conf, xPos, 80);

//			yPos+=16;

//			String ms2String = ms2No==1 ? "spectrum " : "spectra";
//			ms2String = ms2No+" MS2 "+ms2String;
//			g2.drawString(ms2String, 4, yPos);


		g2.setFont(statusFont);
		GuiUtils.drawListStatusElement(ec.isComputing(), g2, this);
	}

}