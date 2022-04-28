package com.mirakl.hybris.promotions.hmc;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import de.hybris.platform.hmc.AbstractEditorMenuChip;
import de.hybris.platform.hmc.AbstractExplorerMenuTreeNodeChip;
import de.hybris.platform.hmc.EditorTabChip;
import de.hybris.platform.hmc.extension.HMCExtension;
import de.hybris.platform.hmc.extension.MenuEntrySlotEntry;
import de.hybris.platform.hmc.generic.ClipChip;
import de.hybris.platform.hmc.generic.ToolbarActionChip;
import de.hybris.platform.hmc.webchips.Chip;
import de.hybris.platform.hmc.webchips.DisplayState;


public class MiraklPromotionsHMCExtension extends HMCExtension {
  /** Edit the local|project.properties to change logging behavior (properties log4j.*). */
  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(MiraklPromotionsHMCExtension.class.getName());

  /** Path to the resource bundles. */
  public static final String RESOURCE_PATH = "com.mirakl.hybris.promotions.hmc.locales";


  /**
   * @see HMCExtension#getTreeNodeChips(de.hybris.platform.hmc.webchips.DisplayState, de.hybris.platform.hmc.webchips.Chip)
   */
  @Override
  public List<AbstractExplorerMenuTreeNodeChip> getTreeNodeChips(final DisplayState displayState, final Chip parent) {
    return Collections.emptyList();
  }

  /**
   * @see HMCExtension#getMenuEntrySlotEntries(de.hybris.platform.hmc.webchips.DisplayState, de.hybris.platform.hmc.webchips.Chip)
   */
  @Override
  public List<MenuEntrySlotEntry> getMenuEntrySlotEntries(final DisplayState displayState, final Chip parent) {
    return Collections.emptyList();
  }

  /**
   * @see HMCExtension#getSectionChips(de.hybris.platform.hmc.webchips.DisplayState, de.hybris.platform.hmc.generic.ClipChip)
   */
  @Override
  public List<ClipChip> getSectionChips(final DisplayState displayState, final ClipChip parent) {
    return Collections.emptyList();
  }

  @Override
  public List<EditorTabChip> getEditorTabChips(final DisplayState displayState, final AbstractEditorMenuChip parent) {
    return Collections.emptyList();
  }

  /**
   * @see HMCExtension#getToolbarActionChips(de.hybris.platform.hmc.webchips.DisplayState, de.hybris.platform.hmc.webchips.Chip)
   */
  @Override
  public List<ToolbarActionChip> getToolbarActionChips(final DisplayState displayState, final Chip parent) {
    return Collections.emptyList();
  }

  @Override
  public ResourceBundle getLocalizeResourceBundle(final Locale locale) {
    return null;
  }

  @Override
  public String getResourcePath() {
    return RESOURCE_PATH;
  }
}
