package com.pms.models.validation;

import jakarta.validation.groups.Default;

/**
 * Validation group applied only on create operations.
 *
 * Extends Default so that using @Validated(OnCreate.class) on insert
 * runs BOTH the standard field constraints (Default group) AND the
 * password constraints (OnCreate group) in a single pass.
 *
 * Using plain @Validated on update activates only Default, which
 * intentionally skips all constraints marked groups = OnCreate.class.
 */
public interface OnCreate extends Default {}
