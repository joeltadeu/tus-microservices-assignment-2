package com.pms.models.validation;

import jakarta.validation.groups.Default;


/**
 * Validation group applied only on update operations.
 *
 * Extends Default so all standard field constraints run.
 * Does NOT extend OnCreate, so password constraints (groups = OnCreate.class)
 * are completely excluded when @Validated(OnUpdate.class) is used.
 */
public interface OnUpdate extends Default {}
