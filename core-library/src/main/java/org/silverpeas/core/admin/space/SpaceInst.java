/*
 * Copyright (C) 2000 - 2024 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "https://www.silverpeas.org/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.silverpeas.core.admin.space;

import org.silverpeas.core.BasicIdentifier;
import org.silverpeas.core.Identifiable;
import org.silverpeas.core.admin.component.model.*;
import org.silverpeas.core.admin.quota.constant.QuotaType;
import org.silverpeas.core.admin.quota.exception.QuotaException;
import org.silverpeas.core.admin.quota.exception.QuotaRuntimeException;
import org.silverpeas.core.admin.quota.model.Quota;
import org.silverpeas.core.admin.service.OrganizationControllerProvider;
import org.silverpeas.core.admin.space.quota.ComponentSpaceQuotaKey;
import org.silverpeas.core.admin.space.quota.DataStorageSpaceQuotaKey;
import org.silverpeas.core.admin.user.model.User;
import org.silverpeas.core.admin.user.model.UserDetail;
import org.silverpeas.core.contribution.model.WithPermanentLink;
import org.silverpeas.core.i18n.AbstractI18NBean;
import org.silverpeas.core.i18n.I18NHelper;
import org.silverpeas.core.i18n.LocalizedResource;
import org.silverpeas.core.security.Securable;
import org.silverpeas.core.security.authorization.SpaceAccessControl;
import org.silverpeas.core.template.SilverpeasTemplate;
import org.silverpeas.core.template.SilverpeasTemplates;
import org.silverpeas.core.util.URLUtil;
import org.silverpeas.core.util.UnitUtil;
import org.silverpeas.core.util.memory.MemoryUnit;
import org.silverpeas.kernel.annotation.NonNull;
import org.silverpeas.kernel.annotation.Nullable;
import org.silverpeas.kernel.bundle.ResourceLocator;
import org.silverpeas.kernel.cache.model.SimpleCache;
import org.silverpeas.kernel.util.StringUtil;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.silverpeas.core.admin.space.SpaceServiceProvider.getComponentSpaceQuotaService;
import static org.silverpeas.core.admin.space.SpaceServiceProvider.getDataStorageSpaceQuotaService;
import static org.silverpeas.core.cache.service.CacheAccessorProvider.getThreadCacheAccessor;
import static org.silverpeas.kernel.util.StringUtil.isDefined;

/**
 * The class SpaceInst is the representation in memory of a space
 */
public class SpaceInst extends AbstractI18NBean<SpaceI18N>
    implements Serializable, Identifiable, Securable, LocalizedResource, WithPermanentLink {

  public static final String SPACE_KEY_PREFIX = "WA";
  public static final String PERSONAL_SPACE_ID = "-10";
  public static final String DEFAULT_SPACE_ID = "-20";
  private static final long serialVersionUID = 4695928610067045964L;
  // First page possible types
  public static final int FP_TYPE_STANDARD = SpaceHomePageType.STANDARD.ordinal(); // Page d'accueil standard
  public static final int FP_TYPE_COMPONENT_INST = SpaceHomePageType.COMPONENT_INST.ordinal(); // Composant (dans ce cas
  // : firstPageExtraParam = composant instance ID)
  public static final int FP_TYPE_PORTLET = SpaceHomePageType.PORTLET.ordinal(); // Portlets
  public static final int FP_TYPE_HTML_PAGE = SpaceHomePageType.HTML_PAGE.ordinal(); // Page HTML
  public static final String STATUS_REMOVED = "R";
  public static final String QUOTA_STORAGE_PREFIX_KEY = SpaceInst.class + "@DataStorageQuota@";
  public static final String QUOTA_STORAGE_REACHED_PREFIX_KEY = SpaceInst.class + "@ReachedDataStorageQuota@";
  public static final String QUOTA_COMPONENT_PREFIX_KEY = SpaceInst.class + "@ComponentQuota@";
  public static final String QUOTA_COMPONENT_REACHED_PREFIX_KEY = SpaceInst.class + "@ReachedComponentQuota@";

  /* Unique identifier of the space */
  private String id;

  /* Unique identifier of the father of the space */
  private String domainFatherId;

  /* User id of the creator of the space */
  private String creatorUserId;

  /* First page type of the space */
  private int firstPageType;

  /* First page extra param of the space */
  private String firstPageExtraParam;

  /* Space relative order */
  private int orderNum;
  private Date createDate = null;
  private Date updateDate = null;
  private Date removeDate = null;
  private String status = null;
  private UserDetail creator;
  private UserDetail updater;
  private UserDetail remover;
  private String updaterUserId;
  private String removerUserId;
  private boolean inheritanceBlocked = false;
  private String look = null;
  private final SpaceInstLazyDataLoader data = new SpaceInstLazyDataLoader(this);

  /* Array of space ids that are children of this space */
  private int level;
  private boolean displaySpaceFirst;
  private boolean isPersonalSpace;

  @Override
  protected Class<SpaceI18N> getTranslationType() {
    return SpaceI18N.class;
  }

  /**
   * Constructor
   */
  public SpaceInst() {
    id = "";
    domainFatherId = "";
    creatorUserId = "";
    firstPageType = 0;
    firstPageExtraParam = "";
    orderNum = 0;
    level = 0;
    displaySpaceFirst = true;
    isPersonalSpace = false;
  }

  /**
   * Constructs a new collaboration space by copying the specified one. Only the identifier isn't
   * copied as it should be unique.
   * @param si the collaboration space to copy.
   */
  @SuppressWarnings({"IncompleteCopyConstructor", "CopyConstructorMissesField"})
  public SpaceInst(final SpaceInst si) {
    super(si);
    setDescription(si.getDescription());
    setDisplaySpaceFirst(si.displaySpaceFirst);
    setFirstPageExtraParam(si.firstPageExtraParam);
    setFirstPageType(si.firstPageType);
    setInheritanceBlocked(si.inheritanceBlocked);
    setLook(si.look);
    setName(si.getName());
    setLanguage(si.getLanguage());
    setPersonalSpace(si.isPersonalSpace);

    si.getTranslations().values()
        .forEach(this::addTranslation);
  }

  @Override
  @SuppressWarnings("unchecked")
  public BasicIdentifier getIdentifier() {
    return new BasicIdentifier(getLocalId(), getId());
  }

  /**
   * Set the space local id
   *
   * @param id new space local id
   */
  public void setLocalId(int id) {
    this.id = String.valueOf(id);
  }

  public int getLocalId() {
    return StringUtil.isDefined(id) ? Integer.parseInt(id) : -1;
  }

  /**
   * Get the absolute space id
   *
   * @return the requested space id
   */
  @Override
  public String getId() {
    return SPACE_KEY_PREFIX + id;
  }

  /**
   * Set the space father id
   *
   * @param sDomainFatherId The space father id
   */
  public void setDomainFatherId(String sDomainFatherId) {
    domainFatherId = sDomainFatherId;
  }

  /**
   * Get the domain father id
   *
   * @return the space father id. If space has no father, returns an empty string.
   */
  public String getDomainFatherId() {
    return domainFatherId;
  }

  @Override
  public String getName(String language) {

    if (isPersonalSpace) {
      return ResourceLocator.getGeneralLocalizationBundle(language)
          .getString("GML.personalSpace");
    }

    return super.getName(language);
  }

  /**
   * Set the space creator id
   *
   * @param sCreatorUserId The user id of person who created the space
   */
  public void setCreatorUserId(String sCreatorUserId) {
    creatorUserId = sCreatorUserId;
    creator = null;
  }

  /**
   * Get the space creator id
   *
   * @return The user id of person who created the space
   */
  public String getCreatorUserId() {
    return creatorUserId;
  }

  /**
   * Set the space first page type
   *
   * @param iFirstPageType type of the home page of the space.
   */
  public void setFirstPageType(int iFirstPageType) {
    firstPageType = iFirstPageType;
  }

  /**
   * Get the space first page type
   *
   * @return The space first page type
   */
  public int getFirstPageType() {
    return firstPageType;
  }

  /**
   * Set the space relative order num
   *
   * @param iOrderNum the order of the space relative to others
   */
  public void setOrderNum(int iOrderNum) {
    orderNum = iOrderNum;
  }

  /**
   * Get the space relative order num
   *
   * @return the order of the space relative to the others spaces.
   */
  public int getOrderNum() {
    return orderNum;
  }

  /**
   * Set the space first page extra parameter
   *
   * @param sFirstPageExtraParam parameters to pass to the home page of the space.
   */
  public void setFirstPageExtraParam(String sFirstPageExtraParam) {
    firstPageExtraParam = sFirstPageExtraParam;
  }

  /**
   * Get the space first page extra parameter
   *
   * @return The space first page extra parameter
   */
  public String getFirstPageExtraParam() {
    return firstPageExtraParam;
  }

  public List<SpaceInst> getSubSpaces() {
    return data.safeRead(SpaceInstLazyDataLoader.SafeDataAccessor::getCopyOfSubSpaces);
  }

  public void copyDataFrom(final SpaceInst other) {
    data.copy(other.data);
  }

  public void setData(final List<SpaceProfileInst> profiles, final List<String> subSpaceIds,
      final List<String> componentIds) {
    resetData();
    data.manualWrite(d -> {
      d.getProfiles().addAll(profiles);
      d.getSubSpaceIds().addAll(subSpaceIds);
      d.getComponentIds().addAll(componentIds);
    });
  }

  public void resetData() {
    data.reset();
  }

  /**
   * Add a component in component list (WARNING : component will not be added in database, only in
   * that spaceInst object !!!)
   *
   * @param componentInst component instance to be added
   */
  public void addComponentInst(ComponentInst componentInst) {
    data.safeWrite(d -> {
      final String cId = componentInst.getId();
      final List<String> c = d.getComponentIds();
      if (!c.contains(cId)) {
        c.add(cId);
      }
    });
  }

  /**
   * Remove a component from component list (WARNING : component will not be removed from database,
   * only in that spaceInst object !!!)
   *
   * @param componentInst component instance to be removed
   */
  @SuppressWarnings("unused")
  public void deleteComponentInst(ComponentInst componentInst) {
    final String cId = componentInst.getId();
    data.safeWrite(d -> d.getComponentIds().removeIf(cId::equals));
  }

  /**
   * Get all the components in that space.
   * <p>This method handles only {@link SilverpeasSharedComponentInstance} instances.</p>
   * <p>To get also {@link SilverpeasPersonalComponentInstance}, please use
   * {@link #getAllComponentInstances()}</p>
   *
   * @return The components in that space
   */
  public List<ComponentInst> getAllComponentsInst() {
    return data.safeRead(SpaceInstLazyDataLoader.SafeDataAccessor::getCopyOfComponents);
  }

  /**
   * Get all {@link SilverpeasComponentInstance} of the given space.
   * @return a list of {@link SilverpeasComponentInstance} which could be empty but never null.
   */
  public List<SilverpeasComponentInstance> getAllComponentInstances() {
    final List<SilverpeasComponentInstance> componentInstances = data.safeRead(
        d -> d.getCopyOfComponents()
            .stream()
            .map(SilverpeasComponentInstance.class::cast)
            .collect(Collectors.toList()));
    if (isPersonalSpace()) {
      PersonalComponent.getAll()
          .forEach(p -> componentInstances.add(PersonalComponentInstance.from(getCreator(), p)));
    }
    return componentInstances;
  }

  /**
   * Add a space profile in space profile list (WARNING : space profile will not be added in
   * database, only in that spaceInst object !!!)
   *
   * @param spaceProfileInst space profile to be added
   */
  public void addSpaceProfileInst(SpaceProfileInst spaceProfileInst) {
    spaceProfileInst.setSpaceFatherId(String.valueOf(getLocalId()));
    data.safeWrite(d -> {
      final List<SpaceProfileInst> p = d.getProfiles();
      if (isDefined(spaceProfileInst.getId())) {
        p.removeIf(i -> i.getId().equals(spaceProfileInst.getId()));
      } else {
        p.remove(spaceProfileInst);
      }
      p.add(spaceProfileInst);
    });
  }

  /**
   * Remove a space profile from space profile list (WARNING : space profile will not be removed
   * from database, only from that spaceInst object !!!)
   *
   * @param spaceProfileInst space profile to be removed
   */
  public void deleteSpaceProfileInst(SpaceProfileInst spaceProfileInst) {
    data.safeWrite(d ->
        d.getProfiles().removeIf(i -> i.getId().equals(spaceProfileInst.getId())));
  }

  /**
   * Get all the user profiles defined for this space instance.
   *
   * @return an unmodifiable list of user profiles for this space instance.
   */
  public List<SpaceProfileInst> getAllSpaceProfilesInst() {
    return data.safeRead(SpaceInstLazyDataLoader.SafeDataAccessor::getCopyOfProfiles);
  }

  /**
   * Gets both the inherited and the direct space profile instances with the specified role name.
   * @param spaceProfileName the name of the space profile to get.
   * @return a list of either no profile instances (the profile is inherited and not yet cached)
   * or of one or two profile instances whose one is the space-specific profile and the other is the
   * inherited one from the parent spaces.
   */
  public List<SpaceProfileInst> getSpaceProfileInst(String spaceProfileName) {
    return data.safeRead(d ->
        d.streamProfiles()
            .filter(i -> i.getName().equals(spaceProfileName))
            .collect(Collectors.toList()));
  }

  /**
   * Get a non inherited space profile from space profiles list, given its name (WARNING: if more
   * than one space profile match the given name, the first one will be returned).
   * @param spaceProfileName name of requested space profile
   * @return a space profile instance or null if no such profile exists.
   */
  @Nullable
  public SpaceProfileInst getDirectSpaceProfileInst(String spaceProfileName) {
    return getSpaceProfileInst(spaceProfileName, false);
  }

  @Nullable
  public SpaceProfileInst getInheritedSpaceProfileInst(String spaceProfileName) {
    return getSpaceProfileInst(spaceProfileName, true);
  }

  private SpaceProfileInst getSpaceProfileInst(String spaceProfileName, boolean inherited) {
    return data.safeRead(d ->
        d.streamProfiles()
            .filter(i -> i.isInherited() == inherited)
            .filter(i -> i.getName().equals(spaceProfileName))
            .findFirst()
            .orElse(null));
  }

  public List<SpaceProfileInst> getInheritedProfiles() {
    return data.safeRead(d ->
        d.streamProfiles()
            .filter(SpaceProfileInst::isInherited)
            .collect(Collectors.toList()));
  }

  /**
   * Gets the specific right profiles of this space instance (that is to say all the
   * non-inherited profiles)
   * @return the specific right profiles of this space instance;
   */
  public List<SpaceProfileInst> getProfiles() {
    return data.safeRead(d ->
        d.streamProfiles()
            .filter(Predicate.not(SpaceProfileInst::isInherited))
            .collect(Collectors.toList()));
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return this.level;
  }

  public boolean isRoot() {
    return (!StringUtil.isDefined(getDomainFatherId()) || "0".equals(getDomainFatherId()) || "-1"
        .equals(getDomainFatherId()));
  }

  @Override
  public Date getCreationDate() {
    return createDate;
  }

  public void setCreationDate(Date createDate) {
    this.createDate = createDate;
  }

  public Date getRemovalDate() {
    return removeDate;
  }

  public void setRemovalDate(Date removeDate) {
    this.removeDate = removeDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public Date getLastUpdateDate() {
    return updateDate;
  }

  public void setLastUpdate(Date updateDate) {
    this.updateDate = updateDate;
  }

  public String getRemoverUserId() {
    return removerUserId;
  }

  public void setRemoverUserId(String removerUserId) {
    this.removerUserId = removerUserId;
    remover = null;
  }

  public String getUpdaterUserId() {
    return updaterUserId;
  }

  public void setUpdaterUserId(String updaterUserId) {
    this.updaterUserId = updaterUserId;
    updater = null;
  }

  public boolean isInheritanceBlocked() {
    return inheritanceBlocked;
  }

  public void setInheritanceBlocked(boolean isInheritanceBlocked) {
    this.inheritanceBlocked = isInheritanceBlocked;
  }

  public String getLook() {
    return look;
  }

  public void setLook(String look) {
    this.look = look;
  }

  @Override
  public User getCreator() {
    if (creator == null && isDefined(creatorUserId)) {
      creator = UserDetail.getById(creatorUserId);
    }
    return creator;
  }

  @Override
  public User getLastUpdater() {
    if (updater == null && isDefined(updaterUserId)) {
      updater = UserDetail.getById(updaterUserId);
    }
    return updater;
  }

  public User getRemover() {
    if (remover == null && isDefined(removerUserId)) {
      remover = UserDetail.getById(removerUserId);
    }
    return remover;
  }

  public boolean isDisplaySpaceFirst() {
    return displaySpaceFirst;
  }

  public void setDisplaySpaceFirst(boolean isDisplaySpaceFirst) {
    this.displaySpaceFirst = isDisplaySpaceFirst;
  }

  public void setPersonalSpace(boolean isPersonalSpace) {
    this.isPersonalSpace = isPersonalSpace;
  }

  public boolean isPersonalSpace() {
    return isPersonalSpace;
  }

  /**
   * @return the componentSpaceQuota
   */
  public Quota getComponentSpaceQuota() {
    final SimpleCache cache = getThreadCacheAccessor().getCache();
    return cache.computeIfAbsent(QUOTA_COMPONENT_PREFIX_KEY + getLocalId(), Quota.class, () -> {
      try {
        return getComponentSpaceQuotaService().get(ComponentSpaceQuotaKey.from(this));
      } catch (final QuotaException qe) {
        throw new QuotaRuntimeException("Cannot get quota for space", qe);
      }
    });
  }

  public void clearComponentSpaceQuotaCache() {
    getThreadCacheAccessor().getCache().remove(QUOTA_COMPONENT_PREFIX_KEY + getLocalId());
  }

  /**
   * Sets the max count of component space of the space
   */
  public void setComponentSpaceQuotaMaxCount(final long componentSpaceQuotaMaxCount)
      throws QuotaException {
    final Quota componentSpaceQuota = getComponentSpaceQuota();
    componentSpaceQuota.setMaxCount(componentSpaceQuotaMaxCount);
    componentSpaceQuota.validateBounds();
  }

  /**
   * @return the componentSpaceQuota
   */
  @NonNull
  private Quota getReachedComponentSpaceQuota() {
    final SimpleCache cache = getThreadCacheAccessor().getCache();
    Quota quota =  cache.computeIfAbsent(QUOTA_COMPONENT_REACHED_PREFIX_KEY + getLocalId(),
        Quota.class,
        () -> getComponentSpaceQuotaService()
            .getQuotaReachedFromSpacePath(ComponentSpaceQuotaKey.from(this)));
    return Objects.requireNonNull(quota);
  }

  /**
   * Indicates if the quota of the space or of a parent space is reached.
   *
   * @return true if the quota in storage space of the parent of this component instance is reached.
   * False otherwise.
   */
  public boolean isComponentSpaceQuotaReached() {
    return getReachedComponentSpaceQuota().isReached();
  }

  /**
   * Gets the error message about reached quota space of this component's father.
   *
   * @param language an ISO 631-1 code of the language.
   * @return a localized error message to indicate a storage space quota is reached.
   */
  public String getComponentSpaceQuotaReachedErrorMessage(final String language) {
    return getQuotaReachedErrorMessage(getReachedComponentSpaceQuota(), language, "componentSpaceQuotaReached");
  }

  /**
   * @return the dataStorageQuota
   */
  public Quota getDataStorageQuota() {
    final SimpleCache cache = getThreadCacheAccessor().getCache();
    return cache.computeIfAbsent(QUOTA_STORAGE_PREFIX_KEY + getLocalId(), Quota.class, () -> {
      try {
        return getDataStorageSpaceQuotaService().get(DataStorageSpaceQuotaKey.from(this));
      } catch (final QuotaException qe) {
        throw new QuotaRuntimeException("Cannot get quota for data storage", qe);
      }
    });
  }

  public void clearDataStorageQuotaCache() {
    getThreadCacheAccessor().getCache().remove(QUOTA_STORAGE_PREFIX_KEY + getLocalId());
  }

  /**
   * Sets the max count of data storage of the space
   */
  public void setDataStorageQuotaMaxCount(final long dataStorageQuotaMaxCount)
      throws QuotaException {
    final Quota dataStorageQuota = getDataStorageQuota();
    dataStorageQuota.setMaxCount(dataStorageQuotaMaxCount);
    dataStorageQuota.validateBounds();
  }

  /**
   * @return the dataStorageQuota
   */
  @NonNull
  private Quota getReachedDataStorageQuota() {
    final SimpleCache cache = getThreadCacheAccessor().getCache();
    Quota quota =  cache.computeIfAbsent(QUOTA_STORAGE_REACHED_PREFIX_KEY + getLocalId(),
        Quota.class,
        () -> getDataStorageSpaceQuotaService()
            .getQuotaReachedFromSpacePath(DataStorageSpaceQuotaKey.from(this)));
    return Objects.requireNonNull(quota);
  }

  /**
   * Indicates if the quota of the space or of a parent space is reached.
   *
   * @return true if the storage space is reached for this component instance. False otherwise.
   */
  public boolean isDataStorageQuotaReached() {
    return getReachedDataStorageQuota().isReached();
  }

  /**
   * Gets the error message about data storage space quota reached.
   *
   * @param language the ISO 631-1 code of the language.
   * @return the localized error message for a reach storage quota space for a component instance.
   */
  public String getDataStorageQuotaReachedErrorMessage(final String language) {
    return getQuotaReachedErrorMessage(getReachedDataStorageQuota(), language, "dataStorageQuotaReached");
  }

  /**
   * Centralized the error message about reached quota.
   *
   * @param quotaReached a quota
   * @param language an ISO 631-1 language code.
   * @param stringTemplateFile a template file to use for the error message.
   * @return the error message.
   */
  private String getQuotaReachedErrorMessage(Quota quotaReached, String language,
      final String stringTemplateFile) {
    if (!QuotaType.COMPONENTS_IN_SPACE.equals(quotaReached.getType())) {
      quotaReached = new Quota(quotaReached);
      quotaReached.setMinCount(
          UnitUtil.convertTo(quotaReached.getMinCount(), MemoryUnit.B, MemoryUnit.MB));
      quotaReached.setMaxCount(
          UnitUtil.convertTo(quotaReached.getMaxCount(), MemoryUnit.B, MemoryUnit.MB));
      quotaReached.setCount(
          UnitUtil.convertTo(quotaReached.getCount(), MemoryUnit.B, MemoryUnit.MB));
    }
    SpaceInstLight space = OrganizationControllerProvider.getOrganisationController()
        .getSpaceInstLightById(quotaReached.getResourceId());
    final SilverpeasTemplate template = SilverpeasTemplates.createSilverpeasTemplateOnCore(
        "admin/space/quota");
    template.setAttribute("quota", quotaReached);
    if (space.getLocalId() != new SpaceInstLight(this).getLocalId()) {
      template.setAttribute("fromSpaceId", space.getLocalId());
      template.setAttribute("fromSpaceName", space.getName());
    }
    if (!StringUtil.isDefined(language)) {
      language = I18NHelper.DEFAULT_LANGUAGE;
    }
    return template.applyFileTemplate(stringTemplateFile + "_" + language);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof SpaceInst) {
      SpaceInst other = (SpaceInst) obj;
      return Objects.equals(other.createDate, createDate)
          && Objects.equals(other.id, id)
          && Objects.equals(other.level, level)
          && Objects.equals(other.look, look)
          && Objects.equals(other.firstPageType, firstPageType)
          && Objects.equals(other.orderNum, orderNum)
          && Objects.equals(other.creatorUserId, creatorUserId)
          && Objects.equals(other.getDescription(), getDescription())
          && Objects.equals(other.domainFatherId, domainFatherId)
          && Objects.equals(other.firstPageExtraParam, firstPageExtraParam)
          && Objects.equals(other.getName(), getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(createDate, id, level, look, firstPageType, orderNum,
        creatorUserId, getDescription(), domainFatherId, firstPageExtraParam, getName());
  }

  @Override
  public String getPermalink() {
    return URLUtil.getSimpleURL(URLUtil.URL_SPACE, getId());
  }

  public boolean isRemoved() {
    return STATUS_REMOVED.equals(getStatus());
  }

  @Override
  public boolean canBeAccessedBy(final User user) {
    return SpaceAccessControl.get().isUserAuthorized(user.getId(), getId());
  }

  /**
   * Is the user can modify this collaboration space?
   * @param user a user in Silverpeas.
   * @return true if the user can both access this collaboration space and has management privilege
   * on this space (by being either an administrator or a space manager)
   */
  @Override
  public boolean canBeModifiedBy(final User user) {
    return SpaceAccessControl.get().hasUserSpaceManagementAuthorization(user.getId(), getId());
  }

  /**
   * Can the user add spaces or component instances into this space?
   * @param user a user in Silverpeas.
   * @return true if he can, false otherwise.
   * @see Securable#canBeFiledInBy(User)
   */
  @Override
  public boolean canBeFiledInBy(final User user) {
    return canBeModifiedBy(user);
  }
}
