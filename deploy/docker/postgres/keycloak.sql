--
-- PostgreSQL database dump
--

-- Dumped from database version 15.3 (Debian 15.3-1.pgdg110+1)
-- Dumped by pg_dump version 15.1

-- Started on 2023-05-19 13:52:11 UTC

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 5 (class 2615 OID 2200)
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 214 (class 1259 OID 16387)
-- Name: admin_event_entity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admin_event_entity (
    id character varying(36) NOT NULL,
    admin_event_time bigint,
    realm_id character varying(255),
    operation_type character varying(255),
    auth_realm_id character varying(255),
    auth_client_id character varying(255),
    auth_user_id character varying(255),
    ip_address character varying(255),
    resource_path character varying(2550),
    representation text,
    error character varying(255),
    resource_type character varying(64)
);


--
-- TOC entry 215 (class 1259 OID 16392)
-- Name: associated_policy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.associated_policy (
    policy_id character varying(36) NOT NULL,
    associated_policy_id character varying(36) NOT NULL
);


--
-- TOC entry 216 (class 1259 OID 16395)
-- Name: authentication_execution; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authentication_execution (
    id character varying(36) NOT NULL,
    alias character varying(255),
    authenticator character varying(36),
    realm_id character varying(36),
    flow_id character varying(36),
    requirement integer,
    priority integer,
    authenticator_flow boolean DEFAULT false NOT NULL,
    auth_flow_id character varying(36),
    auth_config character varying(36)
);


--
-- TOC entry 217 (class 1259 OID 16399)
-- Name: authentication_flow; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authentication_flow (
    id character varying(36) NOT NULL,
    alias character varying(255),
    description character varying(255),
    realm_id character varying(36),
    provider_id character varying(36) DEFAULT 'basic-flow'::character varying NOT NULL,
    top_level boolean DEFAULT false NOT NULL,
    built_in boolean DEFAULT false NOT NULL
);


--
-- TOC entry 218 (class 1259 OID 16407)
-- Name: authenticator_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authenticator_config (
    id character varying(36) NOT NULL,
    alias character varying(255),
    realm_id character varying(36)
);


--
-- TOC entry 219 (class 1259 OID 16410)
-- Name: authenticator_config_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authenticator_config_entry (
    authenticator_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


--
-- TOC entry 220 (class 1259 OID 16415)
-- Name: broker_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.broker_link (
    identity_provider character varying(255) NOT NULL,
    storage_provider_id character varying(255),
    realm_id character varying(36) NOT NULL,
    broker_user_id character varying(255),
    broker_username character varying(255),
    token text,
    user_id character varying(255) NOT NULL
);


--
-- TOC entry 221 (class 1259 OID 16420)
-- Name: client; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client (
    id character varying(36) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    full_scope_allowed boolean DEFAULT false NOT NULL,
    client_id character varying(255),
    not_before integer,
    public_client boolean DEFAULT false NOT NULL,
    secret character varying(255),
    base_url character varying(255),
    bearer_only boolean DEFAULT false NOT NULL,
    management_url character varying(255),
    surrogate_auth_required boolean DEFAULT false NOT NULL,
    realm_id character varying(36),
    protocol character varying(255),
    node_rereg_timeout integer DEFAULT 0,
    frontchannel_logout boolean DEFAULT false NOT NULL,
    consent_required boolean DEFAULT false NOT NULL,
    name character varying(255),
    service_accounts_enabled boolean DEFAULT false NOT NULL,
    client_authenticator_type character varying(255),
    root_url character varying(255),
    description character varying(255),
    registration_token character varying(255),
    standard_flow_enabled boolean DEFAULT true NOT NULL,
    implicit_flow_enabled boolean DEFAULT false NOT NULL,
    direct_access_grants_enabled boolean DEFAULT false NOT NULL,
    always_display_in_console boolean DEFAULT false NOT NULL
);


--
-- TOC entry 222 (class 1259 OID 16438)
-- Name: client_attributes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_attributes (
    client_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


--
-- TOC entry 223 (class 1259 OID 16443)
-- Name: client_auth_flow_bindings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_auth_flow_bindings (
    client_id character varying(36) NOT NULL,
    flow_id character varying(36),
    binding_name character varying(255) NOT NULL
);


--
-- TOC entry 224 (class 1259 OID 16446)
-- Name: client_initial_access; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_initial_access (
    id character varying(36) NOT NULL,
    realm_id character varying(36) NOT NULL,
    "timestamp" integer,
    expiration integer,
    count integer,
    remaining_count integer
);


--
-- TOC entry 225 (class 1259 OID 16449)
-- Name: client_node_registrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_node_registrations (
    client_id character varying(36) NOT NULL,
    value integer,
    name character varying(255) NOT NULL
);


--
-- TOC entry 226 (class 1259 OID 16452)
-- Name: client_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_scope (
    id character varying(36) NOT NULL,
    name character varying(255),
    realm_id character varying(36),
    description character varying(255),
    protocol character varying(255)
);


--
-- TOC entry 227 (class 1259 OID 16457)
-- Name: client_scope_attributes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_scope_attributes (
    scope_id character varying(36) NOT NULL,
    value character varying(2048),
    name character varying(255) NOT NULL
);


--
-- TOC entry 228 (class 1259 OID 16462)
-- Name: client_scope_client; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_scope_client (
    client_id character varying(255) NOT NULL,
    scope_id character varying(255) NOT NULL,
    default_scope boolean DEFAULT false NOT NULL
);


--
-- TOC entry 229 (class 1259 OID 16468)
-- Name: client_scope_role_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_scope_role_mapping (
    scope_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);


--
-- TOC entry 230 (class 1259 OID 16471)
-- Name: client_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_session (
    id character varying(36) NOT NULL,
    client_id character varying(36),
    redirect_uri character varying(255),
    state character varying(255),
    "timestamp" integer,
    session_id character varying(36),
    auth_method character varying(255),
    realm_id character varying(255),
    auth_user_id character varying(36),
    current_action character varying(36)
);


--
-- TOC entry 231 (class 1259 OID 16476)
-- Name: client_session_auth_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_session_auth_status (
    authenticator character varying(36) NOT NULL,
    status integer,
    client_session character varying(36) NOT NULL
);


--
-- TOC entry 232 (class 1259 OID 16479)
-- Name: client_session_note; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_session_note (
    name character varying(255) NOT NULL,
    value character varying(255),
    client_session character varying(36) NOT NULL
);


--
-- TOC entry 233 (class 1259 OID 16484)
-- Name: client_session_prot_mapper; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_session_prot_mapper (
    protocol_mapper_id character varying(36) NOT NULL,
    client_session character varying(36) NOT NULL
);


--
-- TOC entry 234 (class 1259 OID 16487)
-- Name: client_session_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_session_role (
    role_id character varying(255) NOT NULL,
    client_session character varying(36) NOT NULL
);


--
-- TOC entry 235 (class 1259 OID 16490)
-- Name: client_user_session_note; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_user_session_note (
    name character varying(255) NOT NULL,
    value character varying(2048),
    client_session character varying(36) NOT NULL
);


--
-- TOC entry 236 (class 1259 OID 16495)
-- Name: component; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.component (
    id character varying(36) NOT NULL,
    name character varying(255),
    parent_id character varying(36),
    provider_id character varying(36),
    provider_type character varying(255),
    realm_id character varying(36),
    sub_type character varying(255)
);


--
-- TOC entry 237 (class 1259 OID 16500)
-- Name: component_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.component_config (
    id character varying(36) NOT NULL,
    component_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(4000)
);


--
-- TOC entry 238 (class 1259 OID 16505)
-- Name: composite_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.composite_role (
    composite character varying(36) NOT NULL,
    child_role character varying(36) NOT NULL
);


--
-- TOC entry 239 (class 1259 OID 16508)
-- Name: credential; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credential (
    id character varying(36) NOT NULL,
    salt bytea,
    type character varying(255),
    user_id character varying(36),
    created_date bigint,
    user_label character varying(255),
    secret_data text,
    credential_data text,
    priority integer
);


--
-- TOC entry 240 (class 1259 OID 16513)
-- Name: databasechangelog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


--
-- TOC entry 241 (class 1259 OID 16518)
-- Name: databasechangeloglock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


--
-- TOC entry 242 (class 1259 OID 16521)
-- Name: default_client_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.default_client_scope (
    realm_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL,
    default_scope boolean DEFAULT false NOT NULL
);


--
-- TOC entry 243 (class 1259 OID 16525)
-- Name: event_entity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.event_entity (
    id character varying(36) NOT NULL,
    client_id character varying(255),
    details_json character varying(2550),
    error character varying(255),
    ip_address character varying(255),
    realm_id character varying(255),
    session_id character varying(255),
    event_time bigint,
    type character varying(255),
    user_id character varying(255)
);


--
-- TOC entry 244 (class 1259 OID 16530)
-- Name: fed_user_attribute; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fed_user_attribute (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36),
    value character varying(2024)
);


--
-- TOC entry 245 (class 1259 OID 16535)
-- Name: fed_user_consent; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fed_user_consent (
    id character varying(36) NOT NULL,
    client_id character varying(255),
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36),
    created_date bigint,
    last_updated_date bigint,
    client_storage_provider character varying(36),
    external_client_id character varying(255)
);


--
-- TOC entry 246 (class 1259 OID 16540)
-- Name: fed_user_consent_cl_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fed_user_consent_cl_scope (
    user_consent_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


--
-- TOC entry 247 (class 1259 OID 16543)
-- Name: fed_user_credential; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fed_user_credential (
    id character varying(36) NOT NULL,
    salt bytea,
    type character varying(255),
    created_date bigint,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36),
    user_label character varying(255),
    secret_data text,
    credential_data text,
    priority integer
);


--
-- TOC entry 248 (class 1259 OID 16548)
-- Name: fed_user_group_membership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fed_user_group_membership (
    group_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


--
-- TOC entry 249 (class 1259 OID 16551)
-- Name: fed_user_required_action; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fed_user_required_action (
    required_action character varying(255) DEFAULT ' '::character varying NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


--
-- TOC entry 250 (class 1259 OID 16557)
-- Name: fed_user_role_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fed_user_role_mapping (
    role_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


--
-- TOC entry 251 (class 1259 OID 16560)
-- Name: federated_identity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.federated_identity (
    identity_provider character varying(255) NOT NULL,
    realm_id character varying(36),
    federated_user_id character varying(255),
    federated_username character varying(255),
    token text,
    user_id character varying(36) NOT NULL
);


--
-- TOC entry 252 (class 1259 OID 16565)
-- Name: federated_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.federated_user (
    id character varying(255) NOT NULL,
    storage_provider_id character varying(255),
    realm_id character varying(36) NOT NULL
);


--
-- TOC entry 253 (class 1259 OID 16570)
-- Name: group_attribute; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.group_attribute (
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255),
    group_id character varying(36) NOT NULL
);


--
-- TOC entry 254 (class 1259 OID 16576)
-- Name: group_role_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.group_role_mapping (
    role_id character varying(36) NOT NULL,
    group_id character varying(36) NOT NULL
);


--
-- TOC entry 255 (class 1259 OID 16579)
-- Name: identity_provider; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_provider (
    internal_id character varying(36) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    provider_alias character varying(255),
    provider_id character varying(255),
    store_token boolean DEFAULT false NOT NULL,
    authenticate_by_default boolean DEFAULT false NOT NULL,
    realm_id character varying(36),
    add_token_role boolean DEFAULT true NOT NULL,
    trust_email boolean DEFAULT false NOT NULL,
    first_broker_login_flow_id character varying(36),
    post_broker_login_flow_id character varying(36),
    provider_display_name character varying(255),
    link_only boolean DEFAULT false NOT NULL
);


--
-- TOC entry 256 (class 1259 OID 16590)
-- Name: identity_provider_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_provider_config (
    identity_provider_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


--
-- TOC entry 257 (class 1259 OID 16595)
-- Name: identity_provider_mapper; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_provider_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    idp_alias character varying(255) NOT NULL,
    idp_mapper_name character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL
);


--
-- TOC entry 258 (class 1259 OID 16600)
-- Name: idp_mapper_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.idp_mapper_config (
    idp_mapper_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


--
-- TOC entry 259 (class 1259 OID 16605)
-- Name: keycloak_group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.keycloak_group (
    id character varying(36) NOT NULL,
    name character varying(255),
    parent_group character varying(36) NOT NULL,
    realm_id character varying(36)
);


--
-- TOC entry 260 (class 1259 OID 16608)
-- Name: keycloak_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.keycloak_role (
    id character varying(36) NOT NULL,
    client_realm_constraint character varying(255),
    client_role boolean DEFAULT false NOT NULL,
    description character varying(255),
    name character varying(255),
    realm_id character varying(255),
    client character varying(36),
    realm character varying(36)
);


--
-- TOC entry 261 (class 1259 OID 16614)
-- Name: migration_model; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.migration_model (
    id character varying(36) NOT NULL,
    version character varying(36),
    update_time bigint DEFAULT 0 NOT NULL
);


--
-- TOC entry 262 (class 1259 OID 16618)
-- Name: offline_client_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.offline_client_session (
    user_session_id character varying(36) NOT NULL,
    client_id character varying(255) NOT NULL,
    offline_flag character varying(4) NOT NULL,
    "timestamp" integer,
    data text,
    client_storage_provider character varying(36) DEFAULT 'local'::character varying NOT NULL,
    external_client_id character varying(255) DEFAULT 'local'::character varying NOT NULL
);


--
-- TOC entry 263 (class 1259 OID 16625)
-- Name: offline_user_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.offline_user_session (
    user_session_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    created_on integer NOT NULL,
    offline_flag character varying(4) NOT NULL,
    data text,
    last_session_refresh integer DEFAULT 0 NOT NULL
);


--
-- TOC entry 264 (class 1259 OID 16631)
-- Name: policy_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.policy_config (
    policy_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


--
-- TOC entry 265 (class 1259 OID 16636)
-- Name: protocol_mapper; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.protocol_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    protocol character varying(255) NOT NULL,
    protocol_mapper_name character varying(255) NOT NULL,
    client_id character varying(36),
    client_scope_id character varying(36)
);


--
-- TOC entry 266 (class 1259 OID 16641)
-- Name: protocol_mapper_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.protocol_mapper_config (
    protocol_mapper_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


--
-- TOC entry 267 (class 1259 OID 16646)
-- Name: realm; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm (
    id character varying(36) NOT NULL,
    access_code_lifespan integer,
    user_action_lifespan integer,
    access_token_lifespan integer,
    account_theme character varying(255),
    admin_theme character varying(255),
    email_theme character varying(255),
    enabled boolean DEFAULT false NOT NULL,
    events_enabled boolean DEFAULT false NOT NULL,
    events_expiration bigint,
    login_theme character varying(255),
    name character varying(255),
    not_before integer,
    password_policy character varying(2550),
    registration_allowed boolean DEFAULT false NOT NULL,
    remember_me boolean DEFAULT false NOT NULL,
    reset_password_allowed boolean DEFAULT false NOT NULL,
    social boolean DEFAULT false NOT NULL,
    ssl_required character varying(255),
    sso_idle_timeout integer,
    sso_max_lifespan integer,
    update_profile_on_soc_login boolean DEFAULT false NOT NULL,
    verify_email boolean DEFAULT false NOT NULL,
    master_admin_client character varying(36),
    login_lifespan integer,
    internationalization_enabled boolean DEFAULT false NOT NULL,
    default_locale character varying(255),
    reg_email_as_username boolean DEFAULT false NOT NULL,
    admin_events_enabled boolean DEFAULT false NOT NULL,
    admin_events_details_enabled boolean DEFAULT false NOT NULL,
    edit_username_allowed boolean DEFAULT false NOT NULL,
    otp_policy_counter integer DEFAULT 0,
    otp_policy_window integer DEFAULT 1,
    otp_policy_period integer DEFAULT 30,
    otp_policy_digits integer DEFAULT 6,
    otp_policy_alg character varying(36) DEFAULT 'HmacSHA1'::character varying,
    otp_policy_type character varying(36) DEFAULT 'totp'::character varying,
    browser_flow character varying(36),
    registration_flow character varying(36),
    direct_grant_flow character varying(36),
    reset_credentials_flow character varying(36),
    client_auth_flow character varying(36),
    offline_session_idle_timeout integer DEFAULT 0,
    revoke_refresh_token boolean DEFAULT false NOT NULL,
    access_token_life_implicit integer DEFAULT 0,
    login_with_email_allowed boolean DEFAULT true NOT NULL,
    duplicate_emails_allowed boolean DEFAULT false NOT NULL,
    docker_auth_flow character varying(36),
    refresh_token_max_reuse integer DEFAULT 0,
    allow_user_managed_access boolean DEFAULT false NOT NULL,
    sso_max_lifespan_remember_me integer DEFAULT 0 NOT NULL,
    sso_idle_timeout_remember_me integer DEFAULT 0 NOT NULL,
    default_role character varying(255)
);


--
-- TOC entry 268 (class 1259 OID 16679)
-- Name: realm_attribute; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_attribute (
    name character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    value text
);


--
-- TOC entry 269 (class 1259 OID 16684)
-- Name: realm_default_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_default_groups (
    realm_id character varying(36) NOT NULL,
    group_id character varying(36) NOT NULL
);


--
-- TOC entry 270 (class 1259 OID 16687)
-- Name: realm_enabled_event_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_enabled_event_types (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


--
-- TOC entry 271 (class 1259 OID 16690)
-- Name: realm_events_listeners; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_events_listeners (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


--
-- TOC entry 272 (class 1259 OID 16693)
-- Name: realm_localizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_localizations (
    realm_id character varying(255) NOT NULL,
    locale character varying(255) NOT NULL,
    texts text NOT NULL
);


--
-- TOC entry 273 (class 1259 OID 16698)
-- Name: realm_required_credential; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_required_credential (
    type character varying(255) NOT NULL,
    form_label character varying(255),
    input boolean DEFAULT false NOT NULL,
    secret boolean DEFAULT false NOT NULL,
    realm_id character varying(36) NOT NULL
);


--
-- TOC entry 274 (class 1259 OID 16705)
-- Name: realm_smtp_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_smtp_config (
    realm_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


--
-- TOC entry 275 (class 1259 OID 16710)
-- Name: realm_supported_locales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_supported_locales (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


--
-- TOC entry 276 (class 1259 OID 16713)
-- Name: redirect_uris; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.redirect_uris (
    client_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


--
-- TOC entry 277 (class 1259 OID 16716)
-- Name: required_action_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.required_action_config (
    required_action_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


--
-- TOC entry 278 (class 1259 OID 16721)
-- Name: required_action_provider; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.required_action_provider (
    id character varying(36) NOT NULL,
    alias character varying(255),
    name character varying(255),
    realm_id character varying(36),
    enabled boolean DEFAULT false NOT NULL,
    default_action boolean DEFAULT false NOT NULL,
    provider_id character varying(255),
    priority integer
);


--
-- TOC entry 279 (class 1259 OID 16728)
-- Name: resource_attribute; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_attribute (
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255),
    resource_id character varying(36) NOT NULL
);


--
-- TOC entry 280 (class 1259 OID 16734)
-- Name: resource_policy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_policy (
    resource_id character varying(36) NOT NULL,
    policy_id character varying(36) NOT NULL
);


--
-- TOC entry 281 (class 1259 OID 16737)
-- Name: resource_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_scope (
    resource_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


--
-- TOC entry 282 (class 1259 OID 16740)
-- Name: resource_server; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_server (
    id character varying(36) NOT NULL,
    allow_rs_remote_mgmt boolean DEFAULT false NOT NULL,
    policy_enforce_mode smallint NOT NULL,
    decision_strategy smallint DEFAULT 1 NOT NULL
);


--
-- TOC entry 283 (class 1259 OID 16745)
-- Name: resource_server_perm_ticket; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_server_perm_ticket (
    id character varying(36) NOT NULL,
    owner character varying(255) NOT NULL,
    requester character varying(255) NOT NULL,
    created_timestamp bigint NOT NULL,
    granted_timestamp bigint,
    resource_id character varying(36) NOT NULL,
    scope_id character varying(36),
    resource_server_id character varying(36) NOT NULL,
    policy_id character varying(36)
);


--
-- TOC entry 284 (class 1259 OID 16750)
-- Name: resource_server_policy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_server_policy (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    type character varying(255) NOT NULL,
    decision_strategy smallint,
    logic smallint,
    resource_server_id character varying(36) NOT NULL,
    owner character varying(255)
);


--
-- TOC entry 285 (class 1259 OID 16755)
-- Name: resource_server_resource; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_server_resource (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255),
    icon_uri character varying(255),
    owner character varying(255) NOT NULL,
    resource_server_id character varying(36) NOT NULL,
    owner_managed_access boolean DEFAULT false NOT NULL,
    display_name character varying(255)
);


--
-- TOC entry 286 (class 1259 OID 16761)
-- Name: resource_server_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_server_scope (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    icon_uri character varying(255),
    resource_server_id character varying(36) NOT NULL,
    display_name character varying(255)
);


--
-- TOC entry 287 (class 1259 OID 16766)
-- Name: resource_uris; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_uris (
    resource_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


--
-- TOC entry 288 (class 1259 OID 16769)
-- Name: role_attribute; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_attribute (
    id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255)
);


--
-- TOC entry 289 (class 1259 OID 16774)
-- Name: scope_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scope_mapping (
    client_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);


--
-- TOC entry 290 (class 1259 OID 16777)
-- Name: scope_policy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scope_policy (
    scope_id character varying(36) NOT NULL,
    policy_id character varying(36) NOT NULL
);


--
-- TOC entry 291 (class 1259 OID 16780)
-- Name: user_attribute; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_attribute (
    name character varying(255) NOT NULL,
    value character varying(255),
    user_id character varying(36) NOT NULL,
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL
);


--
-- TOC entry 292 (class 1259 OID 16786)
-- Name: user_consent; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_consent (
    id character varying(36) NOT NULL,
    client_id character varying(255),
    user_id character varying(36) NOT NULL,
    created_date bigint,
    last_updated_date bigint,
    client_storage_provider character varying(36),
    external_client_id character varying(255)
);


--
-- TOC entry 293 (class 1259 OID 16791)
-- Name: user_consent_client_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_consent_client_scope (
    user_consent_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


--
-- TOC entry 294 (class 1259 OID 16794)
-- Name: user_entity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_entity (
    id character varying(36) NOT NULL,
    email character varying(255),
    email_constraint character varying(255),
    email_verified boolean DEFAULT false NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    federation_link character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    realm_id character varying(255),
    username character varying(255),
    created_timestamp bigint,
    service_account_client_link character varying(255),
    not_before integer DEFAULT 0 NOT NULL
);


--
-- TOC entry 295 (class 1259 OID 16802)
-- Name: user_federation_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_federation_config (
    user_federation_provider_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


--
-- TOC entry 296 (class 1259 OID 16807)
-- Name: user_federation_mapper; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_federation_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    federation_provider_id character varying(36) NOT NULL,
    federation_mapper_type character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL
);


--
-- TOC entry 297 (class 1259 OID 16812)
-- Name: user_federation_mapper_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_federation_mapper_config (
    user_federation_mapper_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


--
-- TOC entry 298 (class 1259 OID 16817)
-- Name: user_federation_provider; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_federation_provider (
    id character varying(36) NOT NULL,
    changed_sync_period integer,
    display_name character varying(255),
    full_sync_period integer,
    last_sync integer,
    priority integer,
    provider_name character varying(255),
    realm_id character varying(36)
);


--
-- TOC entry 299 (class 1259 OID 16822)
-- Name: user_group_membership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_group_membership (
    group_id character varying(36) NOT NULL,
    user_id character varying(36) NOT NULL
);


--
-- TOC entry 300 (class 1259 OID 16825)
-- Name: user_required_action; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_required_action (
    user_id character varying(36) NOT NULL,
    required_action character varying(255) DEFAULT ' '::character varying NOT NULL
);


--
-- TOC entry 301 (class 1259 OID 16829)
-- Name: user_role_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_role_mapping (
    role_id character varying(255) NOT NULL,
    user_id character varying(36) NOT NULL
);


--
-- TOC entry 302 (class 1259 OID 16832)
-- Name: user_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_session (
    id character varying(36) NOT NULL,
    auth_method character varying(255),
    ip_address character varying(255),
    last_session_refresh integer,
    login_username character varying(255),
    realm_id character varying(255),
    remember_me boolean DEFAULT false NOT NULL,
    started integer,
    user_id character varying(255),
    user_session_state integer,
    broker_session_id character varying(255),
    broker_user_id character varying(255)
);


--
-- TOC entry 303 (class 1259 OID 16838)
-- Name: user_session_note; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_session_note (
    user_session character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(2048)
);


--
-- TOC entry 304 (class 1259 OID 16843)
-- Name: username_login_failure; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.username_login_failure (
    realm_id character varying(36) NOT NULL,
    username character varying(255) NOT NULL,
    failed_login_not_before integer,
    last_failure bigint,
    last_ip_failure character varying(255),
    num_failures integer
);


--
-- TOC entry 305 (class 1259 OID 16848)
-- Name: web_origins; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.web_origins (
    client_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


--
-- TOC entry 4122 (class 0 OID 16387)
-- Dependencies: 214
-- Data for Name: admin_event_entity; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.admin_event_entity (id, admin_event_time, realm_id, operation_type, auth_realm_id, auth_client_id, auth_user_id, ip_address, resource_path, representation, error, resource_type) FROM stdin;
\.


--
-- TOC entry 4123 (class 0 OID 16392)
-- Dependencies: 215
-- Data for Name: associated_policy; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.associated_policy (policy_id, associated_policy_id) FROM stdin;
\.


--
-- TOC entry 4124 (class 0 OID 16395)
-- Dependencies: 216
-- Data for Name: authentication_execution; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) FROM stdin;
ddf3a720-b09f-4e49-85de-eda07572ef1b	\N	auth-cookie	ae43bcfc-5430-4b91-987e-d6df1d2396aa	2e2d3905-b3c5-48e5-b482-033a961839e2	2	10	f	\N	\N
a1a50087-2d5f-4346-bf8e-469225af367b	\N	auth-spnego	ae43bcfc-5430-4b91-987e-d6df1d2396aa	2e2d3905-b3c5-48e5-b482-033a961839e2	3	20	f	\N	\N
6efd0da6-fb4f-43da-b43e-9e9d064db699	\N	identity-provider-redirector	ae43bcfc-5430-4b91-987e-d6df1d2396aa	2e2d3905-b3c5-48e5-b482-033a961839e2	2	25	f	\N	\N
df668993-cac7-4ec8-bc32-8ae4adc492d8	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	2e2d3905-b3c5-48e5-b482-033a961839e2	2	30	t	84546d97-c3cc-453b-a2c2-46ba0ab95fb7	\N
ae17f406-4274-43f2-8743-84eaba1e8742	\N	auth-username-password-form	ae43bcfc-5430-4b91-987e-d6df1d2396aa	84546d97-c3cc-453b-a2c2-46ba0ab95fb7	0	10	f	\N	\N
2169cbd1-847f-44ca-b4c0-9b0699efdc3c	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	84546d97-c3cc-453b-a2c2-46ba0ab95fb7	1	20	t	f0810760-3baa-4030-82d4-8d85aa51022c	\N
ca68c8df-f9d8-41cc-a57b-f9a23cddf6de	\N	conditional-user-configured	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f0810760-3baa-4030-82d4-8d85aa51022c	0	10	f	\N	\N
24f9650d-99ad-4d40-8c01-eea93ace80fd	\N	auth-otp-form	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f0810760-3baa-4030-82d4-8d85aa51022c	0	20	f	\N	\N
f829542f-b90e-4b4c-9092-40c59c4f21c5	\N	direct-grant-validate-username	ae43bcfc-5430-4b91-987e-d6df1d2396aa	9d2c5875-9a54-40b9-8557-d043e6fda5b5	0	10	f	\N	\N
1f59d657-8407-4983-a5f6-819c1c1d9ee6	\N	direct-grant-validate-password	ae43bcfc-5430-4b91-987e-d6df1d2396aa	9d2c5875-9a54-40b9-8557-d043e6fda5b5	0	20	f	\N	\N
4d891058-1290-4e23-92ff-47d3ff8a126f	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	9d2c5875-9a54-40b9-8557-d043e6fda5b5	1	30	t	100d2780-4432-4fe1-809d-f5c816638ca8	\N
0f0e0abd-561f-4775-bfb5-0c919c7aef07	\N	conditional-user-configured	ae43bcfc-5430-4b91-987e-d6df1d2396aa	100d2780-4432-4fe1-809d-f5c816638ca8	0	10	f	\N	\N
55d7d6c8-d0fb-45ca-916d-d8a591978601	\N	direct-grant-validate-otp	ae43bcfc-5430-4b91-987e-d6df1d2396aa	100d2780-4432-4fe1-809d-f5c816638ca8	0	20	f	\N	\N
43221ddc-716a-4b2b-916a-1b3245c3ef3a	\N	registration-page-form	ae43bcfc-5430-4b91-987e-d6df1d2396aa	5a03c39d-e61c-459f-9848-db421cdc614e	0	10	t	75cfb0b0-3dc7-4006-8cde-902b1a0ec575	\N
03eb1302-4029-4df4-ac79-8511a4729ce3	\N	registration-user-creation	ae43bcfc-5430-4b91-987e-d6df1d2396aa	75cfb0b0-3dc7-4006-8cde-902b1a0ec575	0	20	f	\N	\N
c4de85bd-0d0a-4a4d-b11a-322b657a190e	\N	registration-profile-action	ae43bcfc-5430-4b91-987e-d6df1d2396aa	75cfb0b0-3dc7-4006-8cde-902b1a0ec575	0	40	f	\N	\N
8d167b5b-1508-41d9-a746-91411d620364	\N	registration-password-action	ae43bcfc-5430-4b91-987e-d6df1d2396aa	75cfb0b0-3dc7-4006-8cde-902b1a0ec575	0	50	f	\N	\N
f41e428c-9b23-4c6d-aa18-84a67b8d6099	\N	registration-recaptcha-action	ae43bcfc-5430-4b91-987e-d6df1d2396aa	75cfb0b0-3dc7-4006-8cde-902b1a0ec575	3	60	f	\N	\N
6fe05e14-2e62-42e6-bb23-0f8093f13888	\N	reset-credentials-choose-user	ae43bcfc-5430-4b91-987e-d6df1d2396aa	922d78d6-3837-4ad6-b1c0-5519c3aa6f13	0	10	f	\N	\N
3e916981-f426-4481-a9d0-8d8682b846dc	\N	reset-credential-email	ae43bcfc-5430-4b91-987e-d6df1d2396aa	922d78d6-3837-4ad6-b1c0-5519c3aa6f13	0	20	f	\N	\N
96a12488-6b3c-4bae-ae75-115e76232b1c	\N	reset-password	ae43bcfc-5430-4b91-987e-d6df1d2396aa	922d78d6-3837-4ad6-b1c0-5519c3aa6f13	0	30	f	\N	\N
8573ceeb-2c99-46cf-a348-d1b472d769f9	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	922d78d6-3837-4ad6-b1c0-5519c3aa6f13	1	40	t	41647436-eb26-4387-883a-f6d8f7fdb521	\N
317c208d-aa78-4d64-8403-e2e51ef63711	\N	conditional-user-configured	ae43bcfc-5430-4b91-987e-d6df1d2396aa	41647436-eb26-4387-883a-f6d8f7fdb521	0	10	f	\N	\N
e3602028-2026-4a2e-a904-f0046dc78659	\N	reset-otp	ae43bcfc-5430-4b91-987e-d6df1d2396aa	41647436-eb26-4387-883a-f6d8f7fdb521	0	20	f	\N	\N
1bcff5e5-8577-4058-accd-fb97942cc640	\N	client-secret	ae43bcfc-5430-4b91-987e-d6df1d2396aa	61023b39-a976-4a31-ad01-1b6d604de92f	2	10	f	\N	\N
1756b198-96c6-4a4f-8b32-cd47ab985fe4	\N	client-jwt	ae43bcfc-5430-4b91-987e-d6df1d2396aa	61023b39-a976-4a31-ad01-1b6d604de92f	2	20	f	\N	\N
f7f55851-3731-4b28-8708-4b15de366685	\N	client-secret-jwt	ae43bcfc-5430-4b91-987e-d6df1d2396aa	61023b39-a976-4a31-ad01-1b6d604de92f	2	30	f	\N	\N
94198855-8eba-4d45-9134-5698a65d020e	\N	client-x509	ae43bcfc-5430-4b91-987e-d6df1d2396aa	61023b39-a976-4a31-ad01-1b6d604de92f	2	40	f	\N	\N
9bc42f36-dc6d-481d-b926-5e31ac8f27d9	\N	idp-review-profile	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1dadebe0-4220-4d06-b104-ba6b037d6cb7	0	10	f	\N	2f1947f7-ba4f-4829-9f4d-f3bb19b8ee3d
53ff7a47-8744-4097-93e7-5dd3ebd6e4b8	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1dadebe0-4220-4d06-b104-ba6b037d6cb7	0	20	t	2ed2b376-ca9b-4e22-94b5-12274e1b4fe9	\N
8662ba06-c3d4-43b6-a732-b7e71e5f2f95	\N	idp-create-user-if-unique	ae43bcfc-5430-4b91-987e-d6df1d2396aa	2ed2b376-ca9b-4e22-94b5-12274e1b4fe9	2	10	f	\N	f800d718-f431-4106-a9ff-5e8a9625a216
c653e531-ded3-448c-9abf-0c991499347b	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	2ed2b376-ca9b-4e22-94b5-12274e1b4fe9	2	20	t	8b8cc972-3694-42ef-a227-35c3b47a627e	\N
ab4c04c2-7eaf-466f-8030-319bafdf7402	\N	idp-confirm-link	ae43bcfc-5430-4b91-987e-d6df1d2396aa	8b8cc972-3694-42ef-a227-35c3b47a627e	0	10	f	\N	\N
df5e52e8-32ed-4ce3-9174-2243664df4e7	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	8b8cc972-3694-42ef-a227-35c3b47a627e	0	20	t	ee7aaab7-1a84-43fc-b73c-55b943d3c80e	\N
adec8d18-fdff-4d52-8e6f-b5596d3d9893	\N	idp-email-verification	ae43bcfc-5430-4b91-987e-d6df1d2396aa	ee7aaab7-1a84-43fc-b73c-55b943d3c80e	2	10	f	\N	\N
d1c93fbf-72f6-4bb0-a510-dc2c0acf374a	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	ee7aaab7-1a84-43fc-b73c-55b943d3c80e	2	20	t	9495e2c4-b52d-4671-ad0a-a99d895b9835	\N
f75b16ea-b31c-4eba-b7f3-8d0cc7b0de64	\N	idp-username-password-form	ae43bcfc-5430-4b91-987e-d6df1d2396aa	9495e2c4-b52d-4671-ad0a-a99d895b9835	0	10	f	\N	\N
5f2886dc-fbe2-4e67-a36b-a02f9a5d751d	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	9495e2c4-b52d-4671-ad0a-a99d895b9835	1	20	t	b9e3704d-df23-4fc3-9e3a-0593bc0dfd14	\N
8d5621cb-220e-400b-8e11-0fcd26c0c17b	\N	conditional-user-configured	ae43bcfc-5430-4b91-987e-d6df1d2396aa	b9e3704d-df23-4fc3-9e3a-0593bc0dfd14	0	10	f	\N	\N
52856961-261b-460d-a659-0d242c1b3e30	\N	auth-otp-form	ae43bcfc-5430-4b91-987e-d6df1d2396aa	b9e3704d-df23-4fc3-9e3a-0593bc0dfd14	0	20	f	\N	\N
2d8414e3-295a-4131-88f5-b4dea91f82ff	\N	http-basic-authenticator	ae43bcfc-5430-4b91-987e-d6df1d2396aa	ca4b5f2f-3a22-4ed7-92a1-e4972d57d505	0	10	f	\N	\N
66d2c0a6-526d-4445-857b-615bc79466eb	\N	docker-http-basic-authenticator	ae43bcfc-5430-4b91-987e-d6df1d2396aa	ecc098a5-5c5f-48ac-8c1b-27f827ad16ee	0	10	f	\N	\N
6df4066f-3b50-4bf8-914b-9d363b614879	\N	no-cookie-redirect	ae43bcfc-5430-4b91-987e-d6df1d2396aa	fd632dde-48e7-4379-a28d-213bff24d7d5	0	10	f	\N	\N
7481bbab-c863-4aef-9dea-e1faed40b023	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	fd632dde-48e7-4379-a28d-213bff24d7d5	0	20	t	c8e96e23-48d9-403b-8d9e-190ff3a7cf15	\N
6a20963f-e92a-4686-b466-c21b2577ecf9	\N	basic-auth	ae43bcfc-5430-4b91-987e-d6df1d2396aa	c8e96e23-48d9-403b-8d9e-190ff3a7cf15	0	10	f	\N	\N
a4c87bdd-738d-4dd4-a43d-6209c57a2821	\N	basic-auth-otp	ae43bcfc-5430-4b91-987e-d6df1d2396aa	c8e96e23-48d9-403b-8d9e-190ff3a7cf15	3	20	f	\N	\N
a11fad35-86d1-46d9-9f3d-24fcb3480735	\N	auth-spnego	ae43bcfc-5430-4b91-987e-d6df1d2396aa	c8e96e23-48d9-403b-8d9e-190ff3a7cf15	3	30	f	\N	\N
8d286ef6-76a7-4e9b-8a45-9b8b0b05068c	\N	auth-cookie	3b82f5f8-9867-4aa1-a600-ae22c220133a	3f99631b-cb27-4204-bc43-f8d64a40daa1	2	10	f	\N	\N
323621db-e4d8-42a7-b5db-48915cfc5473	\N	auth-spnego	3b82f5f8-9867-4aa1-a600-ae22c220133a	3f99631b-cb27-4204-bc43-f8d64a40daa1	3	20	f	\N	\N
9f801887-024e-4c8e-a617-24ef74673b43	\N	identity-provider-redirector	3b82f5f8-9867-4aa1-a600-ae22c220133a	3f99631b-cb27-4204-bc43-f8d64a40daa1	2	25	f	\N	\N
02de0c99-9b31-4d2d-8d49-eeee774b22d0	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	3f99631b-cb27-4204-bc43-f8d64a40daa1	2	30	t	71eb785e-f7cb-4b09-a0d3-8e2c213cb508	\N
9da446ae-ef56-42a0-9b88-77a77eb22fc4	\N	auth-username-password-form	3b82f5f8-9867-4aa1-a600-ae22c220133a	71eb785e-f7cb-4b09-a0d3-8e2c213cb508	0	10	f	\N	\N
7881a280-2afc-4f5e-b824-34cc6d13f2f3	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	71eb785e-f7cb-4b09-a0d3-8e2c213cb508	1	20	t	63349607-698e-4efd-be03-989a1070ee2a	\N
d5c76829-fb5d-48ea-9e64-f1c1dc7b4f68	\N	conditional-user-configured	3b82f5f8-9867-4aa1-a600-ae22c220133a	63349607-698e-4efd-be03-989a1070ee2a	0	10	f	\N	\N
90b5898e-5e28-400b-b6a4-e0537059470e	\N	auth-otp-form	3b82f5f8-9867-4aa1-a600-ae22c220133a	63349607-698e-4efd-be03-989a1070ee2a	0	20	f	\N	\N
48b88bed-3925-411b-bd36-e55988236d78	\N	direct-grant-validate-username	3b82f5f8-9867-4aa1-a600-ae22c220133a	50299e02-9e95-4359-b25a-88974df0e081	0	10	f	\N	\N
2e65460c-eb74-4c25-a147-22311031a8c5	\N	direct-grant-validate-password	3b82f5f8-9867-4aa1-a600-ae22c220133a	50299e02-9e95-4359-b25a-88974df0e081	0	20	f	\N	\N
e958a8b3-0d8c-4a17-a51c-b19696ccd6da	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	50299e02-9e95-4359-b25a-88974df0e081	1	30	t	698400d9-f344-4e44-8699-2f01392119b9	\N
216197ec-ea23-4ca1-9924-19e81a3fed20	\N	conditional-user-configured	3b82f5f8-9867-4aa1-a600-ae22c220133a	698400d9-f344-4e44-8699-2f01392119b9	0	10	f	\N	\N
5df86d9d-f5ac-41fc-aa51-4b09f13efbd4	\N	direct-grant-validate-otp	3b82f5f8-9867-4aa1-a600-ae22c220133a	698400d9-f344-4e44-8699-2f01392119b9	0	20	f	\N	\N
26bafebc-35b4-4e91-9478-0655ccc833d9	\N	registration-page-form	3b82f5f8-9867-4aa1-a600-ae22c220133a	3ff5c90b-0efb-4f57-ac83-495260a0c33f	0	10	t	4fa3b0e1-a2e7-4e45-a058-587624299bf6	\N
ccd66539-9769-4f10-8819-258b99e3a41d	\N	registration-user-creation	3b82f5f8-9867-4aa1-a600-ae22c220133a	4fa3b0e1-a2e7-4e45-a058-587624299bf6	0	20	f	\N	\N
26cd7e34-25c5-44a7-8d71-b6cd55700676	\N	registration-profile-action	3b82f5f8-9867-4aa1-a600-ae22c220133a	4fa3b0e1-a2e7-4e45-a058-587624299bf6	0	40	f	\N	\N
97c61db6-0589-4544-98fd-e30bcf71b1b7	\N	registration-password-action	3b82f5f8-9867-4aa1-a600-ae22c220133a	4fa3b0e1-a2e7-4e45-a058-587624299bf6	0	50	f	\N	\N
3cc486ed-2c18-4de4-b576-255c70fd5506	\N	registration-recaptcha-action	3b82f5f8-9867-4aa1-a600-ae22c220133a	4fa3b0e1-a2e7-4e45-a058-587624299bf6	3	60	f	\N	\N
1c1838f0-e043-460c-89bb-d754ea72f333	\N	reset-credentials-choose-user	3b82f5f8-9867-4aa1-a600-ae22c220133a	45b6fb0c-a07f-455d-a073-9b17d17e82e3	0	10	f	\N	\N
f425ee1c-c0e1-418d-ae65-4d8e0343d2fc	\N	reset-credential-email	3b82f5f8-9867-4aa1-a600-ae22c220133a	45b6fb0c-a07f-455d-a073-9b17d17e82e3	0	20	f	\N	\N
5efc8d79-9064-4528-9c78-471f30d9708f	\N	reset-password	3b82f5f8-9867-4aa1-a600-ae22c220133a	45b6fb0c-a07f-455d-a073-9b17d17e82e3	0	30	f	\N	\N
3bea60c5-a54c-4915-9cca-c1972c006ef5	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	45b6fb0c-a07f-455d-a073-9b17d17e82e3	1	40	t	063af424-87ec-4cf0-a1eb-a8e80ba104fe	\N
fc73279f-a43e-4897-a937-2a311a47f403	\N	conditional-user-configured	3b82f5f8-9867-4aa1-a600-ae22c220133a	063af424-87ec-4cf0-a1eb-a8e80ba104fe	0	10	f	\N	\N
11aa532f-835b-402b-b7b1-28b07770b5c0	\N	reset-otp	3b82f5f8-9867-4aa1-a600-ae22c220133a	063af424-87ec-4cf0-a1eb-a8e80ba104fe	0	20	f	\N	\N
fda331e4-6cc8-4b32-a3ff-5155d3a21bc6	\N	client-secret	3b82f5f8-9867-4aa1-a600-ae22c220133a	d6c380a2-287e-44c4-b103-4ff7d6eaf28f	2	10	f	\N	\N
8ce95ff9-d5bf-4a8e-ab9a-c7ea5b9b38d0	\N	client-jwt	3b82f5f8-9867-4aa1-a600-ae22c220133a	d6c380a2-287e-44c4-b103-4ff7d6eaf28f	2	20	f	\N	\N
df457093-4851-4602-9045-f6b625a71195	\N	client-secret-jwt	3b82f5f8-9867-4aa1-a600-ae22c220133a	d6c380a2-287e-44c4-b103-4ff7d6eaf28f	2	30	f	\N	\N
0a133119-462d-4111-8f47-5cc3dfb81172	\N	client-x509	3b82f5f8-9867-4aa1-a600-ae22c220133a	d6c380a2-287e-44c4-b103-4ff7d6eaf28f	2	40	f	\N	\N
0f5f3861-6d97-4583-8435-afc5b02264bb	\N	idp-review-profile	3b82f5f8-9867-4aa1-a600-ae22c220133a	e4d82e2b-b376-4e4a-a03e-f472e2d15f1e	0	10	f	\N	d76fa61b-a396-41d7-a48e-b819da126a1e
0e87ffb6-5ed2-4036-9826-92686ec4b673	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	e4d82e2b-b376-4e4a-a03e-f472e2d15f1e	0	20	t	19a1b205-6d7a-4779-a60d-4d7541d3c6d2	\N
291b8c71-3e7d-4a34-985e-1588aa587a4f	\N	idp-create-user-if-unique	3b82f5f8-9867-4aa1-a600-ae22c220133a	19a1b205-6d7a-4779-a60d-4d7541d3c6d2	2	10	f	\N	6be19529-7af8-40b2-873c-469fc319381a
554f42be-eca5-43dc-baf5-1c5aad2a8889	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	19a1b205-6d7a-4779-a60d-4d7541d3c6d2	2	20	t	d4d8d2e8-430c-4084-b796-38bc7da5ae48	\N
84761bbb-2631-43d6-bae8-7dddf08096da	\N	idp-confirm-link	3b82f5f8-9867-4aa1-a600-ae22c220133a	d4d8d2e8-430c-4084-b796-38bc7da5ae48	0	10	f	\N	\N
3ce1916f-7e04-4ed8-a634-47dde9c16b73	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	d4d8d2e8-430c-4084-b796-38bc7da5ae48	0	20	t	c02dd250-1b7a-4122-8f4c-872f400789c7	\N
9342c399-e824-4ef4-8c2c-feead71ebaed	\N	idp-email-verification	3b82f5f8-9867-4aa1-a600-ae22c220133a	c02dd250-1b7a-4122-8f4c-872f400789c7	2	10	f	\N	\N
fe88510d-eac4-493f-80be-ad0efcb75b4b	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	c02dd250-1b7a-4122-8f4c-872f400789c7	2	20	t	98d78dba-6e34-4d9b-aed2-923c1587f02d	\N
dd32cfea-8f5d-4edf-aa7f-f6655ba01bda	\N	idp-username-password-form	3b82f5f8-9867-4aa1-a600-ae22c220133a	98d78dba-6e34-4d9b-aed2-923c1587f02d	0	10	f	\N	\N
86732924-5aee-41ab-bfe0-e6c7d0293d2a	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	98d78dba-6e34-4d9b-aed2-923c1587f02d	1	20	t	61cb044e-de8a-4e5e-9bd7-ef392d8c8ba7	\N
dadeffce-fad4-44f5-ad4a-4c12e56f0f76	\N	conditional-user-configured	3b82f5f8-9867-4aa1-a600-ae22c220133a	61cb044e-de8a-4e5e-9bd7-ef392d8c8ba7	0	10	f	\N	\N
79184de9-3956-4504-b7b4-c7baf21a2924	\N	auth-otp-form	3b82f5f8-9867-4aa1-a600-ae22c220133a	61cb044e-de8a-4e5e-9bd7-ef392d8c8ba7	0	20	f	\N	\N
217e7212-2f6e-45be-8abe-8e0395fb9aeb	\N	http-basic-authenticator	3b82f5f8-9867-4aa1-a600-ae22c220133a	cb7fbbec-6206-4e88-b5b4-d3bb02a28849	0	10	f	\N	\N
acdad3a4-d461-4037-aca6-831ae6b4aa09	\N	docker-http-basic-authenticator	3b82f5f8-9867-4aa1-a600-ae22c220133a	cd460e31-c98f-4060-bf9a-e1fa0c5a1a31	0	10	f	\N	\N
0bac2ad6-09be-42c3-97cf-0e74f4b7d583	\N	no-cookie-redirect	3b82f5f8-9867-4aa1-a600-ae22c220133a	ed709be3-22c0-47a6-b5d5-ce6f16e06e89	0	10	f	\N	\N
52e2651d-466c-4627-aaa2-21497c7db2d5	\N	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	ed709be3-22c0-47a6-b5d5-ce6f16e06e89	0	20	t	3ee0e9b4-01e2-4a25-9aa4-6323db6bdf9a	\N
1030c096-cdf1-428c-b277-7129fdd0ff84	\N	basic-auth	3b82f5f8-9867-4aa1-a600-ae22c220133a	3ee0e9b4-01e2-4a25-9aa4-6323db6bdf9a	0	10	f	\N	\N
2df68b48-5ec4-4c25-8189-0566d05abd95	\N	basic-auth-otp	3b82f5f8-9867-4aa1-a600-ae22c220133a	3ee0e9b4-01e2-4a25-9aa4-6323db6bdf9a	3	20	f	\N	\N
470b6358-a0a3-4e86-a7ee-56be1f52f9df	\N	auth-spnego	3b82f5f8-9867-4aa1-a600-ae22c220133a	3ee0e9b4-01e2-4a25-9aa4-6323db6bdf9a	3	30	f	\N	\N
\.


--
-- TOC entry 4125 (class 0 OID 16399)
-- Dependencies: 217
-- Data for Name: authentication_flow; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) FROM stdin;
2e2d3905-b3c5-48e5-b482-033a961839e2	browser	browser based authentication	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	t	t
84546d97-c3cc-453b-a2c2-46ba0ab95fb7	forms	Username, password, otp and other auth forms.	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
f0810760-3baa-4030-82d4-8d85aa51022c	Browser - Conditional OTP	Flow to determine if the OTP is required for the authentication	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
9d2c5875-9a54-40b9-8557-d043e6fda5b5	direct grant	OpenID Connect Resource Owner Grant	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	t	t
100d2780-4432-4fe1-809d-f5c816638ca8	Direct Grant - Conditional OTP	Flow to determine if the OTP is required for the authentication	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
5a03c39d-e61c-459f-9848-db421cdc614e	registration	registration flow	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	t	t
75cfb0b0-3dc7-4006-8cde-902b1a0ec575	registration form	registration form	ae43bcfc-5430-4b91-987e-d6df1d2396aa	form-flow	f	t
922d78d6-3837-4ad6-b1c0-5519c3aa6f13	reset credentials	Reset credentials for a user if they forgot their password or something	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	t	t
41647436-eb26-4387-883a-f6d8f7fdb521	Reset - Conditional OTP	Flow to determine if the OTP should be reset or not. Set to REQUIRED to force.	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
61023b39-a976-4a31-ad01-1b6d604de92f	clients	Base authentication for clients	ae43bcfc-5430-4b91-987e-d6df1d2396aa	client-flow	t	t
1dadebe0-4220-4d06-b104-ba6b037d6cb7	first broker login	Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	t	t
2ed2b376-ca9b-4e22-94b5-12274e1b4fe9	User creation or linking	Flow for the existing/non-existing user alternatives	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
8b8cc972-3694-42ef-a227-35c3b47a627e	Handle Existing Account	Handle what to do if there is existing account with same email/username like authenticated identity provider	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
ee7aaab7-1a84-43fc-b73c-55b943d3c80e	Account verification options	Method with which to verity the existing account	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
9495e2c4-b52d-4671-ad0a-a99d895b9835	Verify Existing Account by Re-authentication	Reauthentication of existing account	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
b9e3704d-df23-4fc3-9e3a-0593bc0dfd14	First broker login - Conditional OTP	Flow to determine if the OTP is required for the authentication	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
ca4b5f2f-3a22-4ed7-92a1-e4972d57d505	saml ecp	SAML ECP Profile Authentication Flow	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	t	t
ecc098a5-5c5f-48ac-8c1b-27f827ad16ee	docker auth	Used by Docker clients to authenticate against the IDP	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	t	t
fd632dde-48e7-4379-a28d-213bff24d7d5	http challenge	An authentication flow based on challenge-response HTTP Authentication Schemes	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	t	t
c8e96e23-48d9-403b-8d9e-190ff3a7cf15	Authentication Options	Authentication options.	ae43bcfc-5430-4b91-987e-d6df1d2396aa	basic-flow	f	t
3f99631b-cb27-4204-bc43-f8d64a40daa1	browser	browser based authentication	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	t	t
71eb785e-f7cb-4b09-a0d3-8e2c213cb508	forms	Username, password, otp and other auth forms.	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
63349607-698e-4efd-be03-989a1070ee2a	Browser - Conditional OTP	Flow to determine if the OTP is required for the authentication	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
50299e02-9e95-4359-b25a-88974df0e081	direct grant	OpenID Connect Resource Owner Grant	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	t	t
698400d9-f344-4e44-8699-2f01392119b9	Direct Grant - Conditional OTP	Flow to determine if the OTP is required for the authentication	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
3ff5c90b-0efb-4f57-ac83-495260a0c33f	registration	registration flow	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	t	t
4fa3b0e1-a2e7-4e45-a058-587624299bf6	registration form	registration form	3b82f5f8-9867-4aa1-a600-ae22c220133a	form-flow	f	t
45b6fb0c-a07f-455d-a073-9b17d17e82e3	reset credentials	Reset credentials for a user if they forgot their password or something	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	t	t
063af424-87ec-4cf0-a1eb-a8e80ba104fe	Reset - Conditional OTP	Flow to determine if the OTP should be reset or not. Set to REQUIRED to force.	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
d6c380a2-287e-44c4-b103-4ff7d6eaf28f	clients	Base authentication for clients	3b82f5f8-9867-4aa1-a600-ae22c220133a	client-flow	t	t
e4d82e2b-b376-4e4a-a03e-f472e2d15f1e	first broker login	Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	t	t
19a1b205-6d7a-4779-a60d-4d7541d3c6d2	User creation or linking	Flow for the existing/non-existing user alternatives	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
d4d8d2e8-430c-4084-b796-38bc7da5ae48	Handle Existing Account	Handle what to do if there is existing account with same email/username like authenticated identity provider	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
c02dd250-1b7a-4122-8f4c-872f400789c7	Account verification options	Method with which to verity the existing account	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
98d78dba-6e34-4d9b-aed2-923c1587f02d	Verify Existing Account by Re-authentication	Reauthentication of existing account	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
61cb044e-de8a-4e5e-9bd7-ef392d8c8ba7	First broker login - Conditional OTP	Flow to determine if the OTP is required for the authentication	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
cb7fbbec-6206-4e88-b5b4-d3bb02a28849	saml ecp	SAML ECP Profile Authentication Flow	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	t	t
cd460e31-c98f-4060-bf9a-e1fa0c5a1a31	docker auth	Used by Docker clients to authenticate against the IDP	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	t	t
ed709be3-22c0-47a6-b5d5-ce6f16e06e89	http challenge	An authentication flow based on challenge-response HTTP Authentication Schemes	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	t	t
3ee0e9b4-01e2-4a25-9aa4-6323db6bdf9a	Authentication Options	Authentication options.	3b82f5f8-9867-4aa1-a600-ae22c220133a	basic-flow	f	t
\.


--
-- TOC entry 4126 (class 0 OID 16407)
-- Dependencies: 218
-- Data for Name: authenticator_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.authenticator_config (id, alias, realm_id) FROM stdin;
2f1947f7-ba4f-4829-9f4d-f3bb19b8ee3d	review profile config	ae43bcfc-5430-4b91-987e-d6df1d2396aa
f800d718-f431-4106-a9ff-5e8a9625a216	create unique user config	ae43bcfc-5430-4b91-987e-d6df1d2396aa
d76fa61b-a396-41d7-a48e-b819da126a1e	review profile config	3b82f5f8-9867-4aa1-a600-ae22c220133a
6be19529-7af8-40b2-873c-469fc319381a	create unique user config	3b82f5f8-9867-4aa1-a600-ae22c220133a
\.


--
-- TOC entry 4127 (class 0 OID 16410)
-- Dependencies: 219
-- Data for Name: authenticator_config_entry; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.authenticator_config_entry (authenticator_id, value, name) FROM stdin;
2f1947f7-ba4f-4829-9f4d-f3bb19b8ee3d	missing	update.profile.on.first.login
f800d718-f431-4106-a9ff-5e8a9625a216	false	require.password.update.after.registration
6be19529-7af8-40b2-873c-469fc319381a	false	require.password.update.after.registration
d76fa61b-a396-41d7-a48e-b819da126a1e	missing	update.profile.on.first.login
\.


--
-- TOC entry 4128 (class 0 OID 16415)
-- Dependencies: 220
-- Data for Name: broker_link; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.broker_link (identity_provider, storage_provider_id, realm_id, broker_user_id, broker_username, token, user_id) FROM stdin;
\.


--
-- TOC entry 4129 (class 0 OID 16420)
-- Dependencies: 221
-- Data for Name: client; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) FROM stdin;
1421d183-2492-4394-b963-a4a8cf677f34	t	f	master-realm	0	f	\N	\N	t	\N	f	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N	0	f	f	master Realm	f	client-secret	\N	\N	\N	t	f	f	f
044054b7-770d-4204-a9fe-3257a210879e	t	f	account	0	t	\N	/realms/master/account/	f	\N	f	ae43bcfc-5430-4b91-987e-d6df1d2396aa	openid-connect	0	f	f	${client_account}	f	client-secret	${authBaseUrl}	\N	\N	t	f	f	f
4cec789f-7e83-4565-9aa1-bda3b05b1adb	t	f	account-console	0	t	\N	/realms/master/account/	f	\N	f	ae43bcfc-5430-4b91-987e-d6df1d2396aa	openid-connect	0	f	f	${client_account-console}	f	client-secret	${authBaseUrl}	\N	\N	t	f	f	f
745a661c-5b71-46f0-b374-1055e8d22ee5	t	f	broker	0	f	\N	\N	t	\N	f	ae43bcfc-5430-4b91-987e-d6df1d2396aa	openid-connect	0	f	f	${client_broker}	f	client-secret	\N	\N	\N	t	f	f	f
d80bf699-b641-4ea2-9752-42cf143ab825	t	f	security-admin-console	0	t	\N	/admin/master/console/	f	\N	f	ae43bcfc-5430-4b91-987e-d6df1d2396aa	openid-connect	0	f	f	${client_security-admin-console}	f	client-secret	${authAdminUrl}	\N	\N	t	f	f	f
6b506029-265e-49f8-b788-c9222f4b7ad5	t	f	admin-cli	0	t	\N	\N	f	\N	f	ae43bcfc-5430-4b91-987e-d6df1d2396aa	openid-connect	0	f	f	${client_admin-cli}	f	client-secret	\N	\N	\N	f	f	t	f
7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	f	AnnetteDemo-realm	0	f	\N	\N	t	\N	f	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N	0	f	f	AnnetteDemo Realm	f	client-secret	\N	\N	\N	t	f	f	f
2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	f	realm-management	0	f	\N	\N	t	\N	f	3b82f5f8-9867-4aa1-a600-ae22c220133a	openid-connect	0	f	f	${client_realm-management}	f	client-secret	\N	\N	\N	t	f	f	f
6cc96394-08b1-48bc-814e-9ed664c4d09c	t	f	account	0	t	\N	/realms/AnnetteDemo/account/	f	\N	f	3b82f5f8-9867-4aa1-a600-ae22c220133a	openid-connect	0	f	f	${client_account}	f	client-secret	${authBaseUrl}	\N	\N	t	f	f	f
da76b89f-97ee-473d-850d-8bb339a8f698	t	f	account-console	0	t	\N	/realms/AnnetteDemo/account/	f	\N	f	3b82f5f8-9867-4aa1-a600-ae22c220133a	openid-connect	0	f	f	${client_account-console}	f	client-secret	${authBaseUrl}	\N	\N	t	f	f	f
4788821b-b889-4cd0-9535-74949e39bc37	t	f	broker	0	f	\N	\N	t	\N	f	3b82f5f8-9867-4aa1-a600-ae22c220133a	openid-connect	0	f	f	${client_broker}	f	client-secret	\N	\N	\N	t	f	f	f
b35f3c52-1869-42f0-8219-694107c37036	t	f	security-admin-console	0	t	\N	/admin/AnnetteDemo/console/	f	\N	f	3b82f5f8-9867-4aa1-a600-ae22c220133a	openid-connect	0	f	f	${client_security-admin-console}	f	client-secret	${authAdminUrl}	\N	\N	t	f	f	f
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	t	f	admin-cli	0	t	\N	\N	f	\N	f	3b82f5f8-9867-4aa1-a600-ae22c220133a	openid-connect	0	f	f	${client_admin-cli}	f	client-secret	\N	\N	\N	f	f	t	f
629e8324-85ac-40be-8940-d6e8ab25eb96	t	t	annette-console	0	t	\N	http://localhost:3000	f		f	3b82f5f8-9867-4aa1-a600-ae22c220133a	openid-connect	-1	t	f	annette-console	f	client-secret	http://localhost:3000		\N	t	f	f	f
\.


--
-- TOC entry 4130 (class 0 OID 16438)
-- Dependencies: 222
-- Data for Name: client_attributes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_attributes (client_id, name, value) FROM stdin;
044054b7-770d-4204-a9fe-3257a210879e	post.logout.redirect.uris	+
4cec789f-7e83-4565-9aa1-bda3b05b1adb	post.logout.redirect.uris	+
4cec789f-7e83-4565-9aa1-bda3b05b1adb	pkce.code.challenge.method	S256
d80bf699-b641-4ea2-9752-42cf143ab825	post.logout.redirect.uris	+
d80bf699-b641-4ea2-9752-42cf143ab825	pkce.code.challenge.method	S256
6cc96394-08b1-48bc-814e-9ed664c4d09c	post.logout.redirect.uris	+
da76b89f-97ee-473d-850d-8bb339a8f698	post.logout.redirect.uris	+
da76b89f-97ee-473d-850d-8bb339a8f698	pkce.code.challenge.method	S256
b35f3c52-1869-42f0-8219-694107c37036	post.logout.redirect.uris	+
b35f3c52-1869-42f0-8219-694107c37036	pkce.code.challenge.method	S256
629e8324-85ac-40be-8940-d6e8ab25eb96	oauth2.device.authorization.grant.enabled	false
629e8324-85ac-40be-8940-d6e8ab25eb96	oidc.ciba.grant.enabled	false
629e8324-85ac-40be-8940-d6e8ab25eb96	backchannel.logout.session.required	true
629e8324-85ac-40be-8940-d6e8ab25eb96	backchannel.logout.revoke.offline.tokens	false
629e8324-85ac-40be-8940-d6e8ab25eb96	display.on.consent.screen	false
629e8324-85ac-40be-8940-d6e8ab25eb96	acr.loa.map	{}
629e8324-85ac-40be-8940-d6e8ab25eb96	use.refresh.tokens	true
629e8324-85ac-40be-8940-d6e8ab25eb96	client_credentials.use_refresh_token	false
629e8324-85ac-40be-8940-d6e8ab25eb96	token.response.type.bearer.lower-case	false
629e8324-85ac-40be-8940-d6e8ab25eb96	tls-client-certificate-bound-access-tokens	false
629e8324-85ac-40be-8940-d6e8ab25eb96	require.pushed.authorization.requests	false
629e8324-85ac-40be-8940-d6e8ab25eb96	post.logout.redirect.uris	http://localhost:3000/*##http://localhost:8500/*
629e8324-85ac-40be-8940-d6e8ab25eb96	client.secret.creation.time	1684411681
\.


--
-- TOC entry 4131 (class 0 OID 16443)
-- Dependencies: 223
-- Data for Name: client_auth_flow_bindings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_auth_flow_bindings (client_id, flow_id, binding_name) FROM stdin;
\.


--
-- TOC entry 4132 (class 0 OID 16446)
-- Dependencies: 224
-- Data for Name: client_initial_access; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_initial_access (id, realm_id, "timestamp", expiration, count, remaining_count) FROM stdin;
\.


--
-- TOC entry 4133 (class 0 OID 16449)
-- Dependencies: 225
-- Data for Name: client_node_registrations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_node_registrations (client_id, value, name) FROM stdin;
\.


--
-- TOC entry 4134 (class 0 OID 16452)
-- Dependencies: 226
-- Data for Name: client_scope; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_scope (id, name, realm_id, description, protocol) FROM stdin;
2f43062e-c262-4ef9-879e-3995f334be6c	offline_access	ae43bcfc-5430-4b91-987e-d6df1d2396aa	OpenID Connect built-in scope: offline_access	openid-connect
cec79e00-452e-4bea-bced-ad2eba9f6cb4	role_list	ae43bcfc-5430-4b91-987e-d6df1d2396aa	SAML role list	saml
af49489d-7ba4-47f1-a754-8411691885dd	profile	ae43bcfc-5430-4b91-987e-d6df1d2396aa	OpenID Connect built-in scope: profile	openid-connect
2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	email	ae43bcfc-5430-4b91-987e-d6df1d2396aa	OpenID Connect built-in scope: email	openid-connect
ceb503d8-d403-48c1-8c6c-dbb4f929b74b	address	ae43bcfc-5430-4b91-987e-d6df1d2396aa	OpenID Connect built-in scope: address	openid-connect
062b2277-882e-4989-9743-97c4485847aa	phone	ae43bcfc-5430-4b91-987e-d6df1d2396aa	OpenID Connect built-in scope: phone	openid-connect
0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	roles	ae43bcfc-5430-4b91-987e-d6df1d2396aa	OpenID Connect scope for add user roles to the access token	openid-connect
52fefbc2-756e-4fa1-935f-4596180dc8d0	web-origins	ae43bcfc-5430-4b91-987e-d6df1d2396aa	OpenID Connect scope for add allowed web origins to the access token	openid-connect
4dae68af-19f6-4918-ae67-6c1fda0827d1	microprofile-jwt	ae43bcfc-5430-4b91-987e-d6df1d2396aa	Microprofile - JWT built-in scope	openid-connect
d41ba78d-a9e7-44cb-a060-e5538a51694c	acr	ae43bcfc-5430-4b91-987e-d6df1d2396aa	OpenID Connect scope for add acr (authentication context class reference) to the token	openid-connect
3887624b-89fe-4d58-83dc-27e34012554d	offline_access	3b82f5f8-9867-4aa1-a600-ae22c220133a	OpenID Connect built-in scope: offline_access	openid-connect
a640577b-c323-4076-9861-10baac3e551b	role_list	3b82f5f8-9867-4aa1-a600-ae22c220133a	SAML role list	saml
93a4cc1a-bddb-4c02-b782-25948d204836	profile	3b82f5f8-9867-4aa1-a600-ae22c220133a	OpenID Connect built-in scope: profile	openid-connect
50d59a29-3563-47de-98c1-361823d6b5c3	email	3b82f5f8-9867-4aa1-a600-ae22c220133a	OpenID Connect built-in scope: email	openid-connect
350748b2-db1c-4d5c-a1a0-d07f7a72878c	address	3b82f5f8-9867-4aa1-a600-ae22c220133a	OpenID Connect built-in scope: address	openid-connect
d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	phone	3b82f5f8-9867-4aa1-a600-ae22c220133a	OpenID Connect built-in scope: phone	openid-connect
ed9a51c3-2280-456c-b474-cc0069829d04	roles	3b82f5f8-9867-4aa1-a600-ae22c220133a	OpenID Connect scope for add user roles to the access token	openid-connect
68dd7df5-09d7-42d1-8b49-c03b890977c5	web-origins	3b82f5f8-9867-4aa1-a600-ae22c220133a	OpenID Connect scope for add allowed web origins to the access token	openid-connect
53251e92-d6fd-4fc0-b8f2-f95428ca676f	microprofile-jwt	3b82f5f8-9867-4aa1-a600-ae22c220133a	Microprofile - JWT built-in scope	openid-connect
8ac4ad99-361d-4825-9733-22bc5888b62c	acr	3b82f5f8-9867-4aa1-a600-ae22c220133a	OpenID Connect scope for add acr (authentication context class reference) to the token	openid-connect
624026aa-d09e-41dc-a080-4dcd0d3af7f2	Person_ID	3b82f5f8-9867-4aa1-a600-ae22c220133a		openid-connect
\.


--
-- TOC entry 4135 (class 0 OID 16457)
-- Dependencies: 227
-- Data for Name: client_scope_attributes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_scope_attributes (scope_id, value, name) FROM stdin;
2f43062e-c262-4ef9-879e-3995f334be6c	true	display.on.consent.screen
2f43062e-c262-4ef9-879e-3995f334be6c	${offlineAccessScopeConsentText}	consent.screen.text
cec79e00-452e-4bea-bced-ad2eba9f6cb4	true	display.on.consent.screen
cec79e00-452e-4bea-bced-ad2eba9f6cb4	${samlRoleListScopeConsentText}	consent.screen.text
af49489d-7ba4-47f1-a754-8411691885dd	true	display.on.consent.screen
af49489d-7ba4-47f1-a754-8411691885dd	${profileScopeConsentText}	consent.screen.text
af49489d-7ba4-47f1-a754-8411691885dd	true	include.in.token.scope
2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	true	display.on.consent.screen
2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	${emailScopeConsentText}	consent.screen.text
2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	true	include.in.token.scope
ceb503d8-d403-48c1-8c6c-dbb4f929b74b	true	display.on.consent.screen
ceb503d8-d403-48c1-8c6c-dbb4f929b74b	${addressScopeConsentText}	consent.screen.text
ceb503d8-d403-48c1-8c6c-dbb4f929b74b	true	include.in.token.scope
062b2277-882e-4989-9743-97c4485847aa	true	display.on.consent.screen
062b2277-882e-4989-9743-97c4485847aa	${phoneScopeConsentText}	consent.screen.text
062b2277-882e-4989-9743-97c4485847aa	true	include.in.token.scope
0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	true	display.on.consent.screen
0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	${rolesScopeConsentText}	consent.screen.text
0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	false	include.in.token.scope
52fefbc2-756e-4fa1-935f-4596180dc8d0	false	display.on.consent.screen
52fefbc2-756e-4fa1-935f-4596180dc8d0		consent.screen.text
52fefbc2-756e-4fa1-935f-4596180dc8d0	false	include.in.token.scope
4dae68af-19f6-4918-ae67-6c1fda0827d1	false	display.on.consent.screen
4dae68af-19f6-4918-ae67-6c1fda0827d1	true	include.in.token.scope
d41ba78d-a9e7-44cb-a060-e5538a51694c	false	display.on.consent.screen
d41ba78d-a9e7-44cb-a060-e5538a51694c	false	include.in.token.scope
3887624b-89fe-4d58-83dc-27e34012554d	true	display.on.consent.screen
3887624b-89fe-4d58-83dc-27e34012554d	${offlineAccessScopeConsentText}	consent.screen.text
a640577b-c323-4076-9861-10baac3e551b	true	display.on.consent.screen
a640577b-c323-4076-9861-10baac3e551b	${samlRoleListScopeConsentText}	consent.screen.text
93a4cc1a-bddb-4c02-b782-25948d204836	true	display.on.consent.screen
93a4cc1a-bddb-4c02-b782-25948d204836	${profileScopeConsentText}	consent.screen.text
93a4cc1a-bddb-4c02-b782-25948d204836	true	include.in.token.scope
50d59a29-3563-47de-98c1-361823d6b5c3	true	display.on.consent.screen
50d59a29-3563-47de-98c1-361823d6b5c3	${emailScopeConsentText}	consent.screen.text
50d59a29-3563-47de-98c1-361823d6b5c3	true	include.in.token.scope
350748b2-db1c-4d5c-a1a0-d07f7a72878c	true	display.on.consent.screen
350748b2-db1c-4d5c-a1a0-d07f7a72878c	${addressScopeConsentText}	consent.screen.text
350748b2-db1c-4d5c-a1a0-d07f7a72878c	true	include.in.token.scope
d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	true	display.on.consent.screen
d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	${phoneScopeConsentText}	consent.screen.text
d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	true	include.in.token.scope
ed9a51c3-2280-456c-b474-cc0069829d04	true	display.on.consent.screen
ed9a51c3-2280-456c-b474-cc0069829d04	${rolesScopeConsentText}	consent.screen.text
ed9a51c3-2280-456c-b474-cc0069829d04	false	include.in.token.scope
68dd7df5-09d7-42d1-8b49-c03b890977c5	false	display.on.consent.screen
68dd7df5-09d7-42d1-8b49-c03b890977c5		consent.screen.text
68dd7df5-09d7-42d1-8b49-c03b890977c5	false	include.in.token.scope
53251e92-d6fd-4fc0-b8f2-f95428ca676f	false	display.on.consent.screen
53251e92-d6fd-4fc0-b8f2-f95428ca676f	true	include.in.token.scope
8ac4ad99-361d-4825-9733-22bc5888b62c	false	display.on.consent.screen
8ac4ad99-361d-4825-9733-22bc5888b62c	false	include.in.token.scope
624026aa-d09e-41dc-a080-4dcd0d3af7f2		consent.screen.text
624026aa-d09e-41dc-a080-4dcd0d3af7f2	true	display.on.consent.screen
624026aa-d09e-41dc-a080-4dcd0d3af7f2	true	include.in.token.scope
624026aa-d09e-41dc-a080-4dcd0d3af7f2		gui.order
\.


--
-- TOC entry 4136 (class 0 OID 16462)
-- Dependencies: 228
-- Data for Name: client_scope_client; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_scope_client (client_id, scope_id, default_scope) FROM stdin;
044054b7-770d-4204-a9fe-3257a210879e	af49489d-7ba4-47f1-a754-8411691885dd	t
044054b7-770d-4204-a9fe-3257a210879e	d41ba78d-a9e7-44cb-a060-e5538a51694c	t
044054b7-770d-4204-a9fe-3257a210879e	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	t
044054b7-770d-4204-a9fe-3257a210879e	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	t
044054b7-770d-4204-a9fe-3257a210879e	52fefbc2-756e-4fa1-935f-4596180dc8d0	t
044054b7-770d-4204-a9fe-3257a210879e	2f43062e-c262-4ef9-879e-3995f334be6c	f
044054b7-770d-4204-a9fe-3257a210879e	4dae68af-19f6-4918-ae67-6c1fda0827d1	f
044054b7-770d-4204-a9fe-3257a210879e	ceb503d8-d403-48c1-8c6c-dbb4f929b74b	f
044054b7-770d-4204-a9fe-3257a210879e	062b2277-882e-4989-9743-97c4485847aa	f
4cec789f-7e83-4565-9aa1-bda3b05b1adb	af49489d-7ba4-47f1-a754-8411691885dd	t
4cec789f-7e83-4565-9aa1-bda3b05b1adb	d41ba78d-a9e7-44cb-a060-e5538a51694c	t
4cec789f-7e83-4565-9aa1-bda3b05b1adb	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	t
4cec789f-7e83-4565-9aa1-bda3b05b1adb	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	t
4cec789f-7e83-4565-9aa1-bda3b05b1adb	52fefbc2-756e-4fa1-935f-4596180dc8d0	t
4cec789f-7e83-4565-9aa1-bda3b05b1adb	2f43062e-c262-4ef9-879e-3995f334be6c	f
4cec789f-7e83-4565-9aa1-bda3b05b1adb	4dae68af-19f6-4918-ae67-6c1fda0827d1	f
4cec789f-7e83-4565-9aa1-bda3b05b1adb	ceb503d8-d403-48c1-8c6c-dbb4f929b74b	f
4cec789f-7e83-4565-9aa1-bda3b05b1adb	062b2277-882e-4989-9743-97c4485847aa	f
6b506029-265e-49f8-b788-c9222f4b7ad5	af49489d-7ba4-47f1-a754-8411691885dd	t
6b506029-265e-49f8-b788-c9222f4b7ad5	d41ba78d-a9e7-44cb-a060-e5538a51694c	t
6b506029-265e-49f8-b788-c9222f4b7ad5	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	t
6b506029-265e-49f8-b788-c9222f4b7ad5	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	t
6b506029-265e-49f8-b788-c9222f4b7ad5	52fefbc2-756e-4fa1-935f-4596180dc8d0	t
6b506029-265e-49f8-b788-c9222f4b7ad5	2f43062e-c262-4ef9-879e-3995f334be6c	f
6b506029-265e-49f8-b788-c9222f4b7ad5	4dae68af-19f6-4918-ae67-6c1fda0827d1	f
6b506029-265e-49f8-b788-c9222f4b7ad5	ceb503d8-d403-48c1-8c6c-dbb4f929b74b	f
6b506029-265e-49f8-b788-c9222f4b7ad5	062b2277-882e-4989-9743-97c4485847aa	f
745a661c-5b71-46f0-b374-1055e8d22ee5	af49489d-7ba4-47f1-a754-8411691885dd	t
745a661c-5b71-46f0-b374-1055e8d22ee5	d41ba78d-a9e7-44cb-a060-e5538a51694c	t
745a661c-5b71-46f0-b374-1055e8d22ee5	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	t
745a661c-5b71-46f0-b374-1055e8d22ee5	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	t
745a661c-5b71-46f0-b374-1055e8d22ee5	52fefbc2-756e-4fa1-935f-4596180dc8d0	t
745a661c-5b71-46f0-b374-1055e8d22ee5	2f43062e-c262-4ef9-879e-3995f334be6c	f
745a661c-5b71-46f0-b374-1055e8d22ee5	4dae68af-19f6-4918-ae67-6c1fda0827d1	f
745a661c-5b71-46f0-b374-1055e8d22ee5	ceb503d8-d403-48c1-8c6c-dbb4f929b74b	f
745a661c-5b71-46f0-b374-1055e8d22ee5	062b2277-882e-4989-9743-97c4485847aa	f
1421d183-2492-4394-b963-a4a8cf677f34	af49489d-7ba4-47f1-a754-8411691885dd	t
1421d183-2492-4394-b963-a4a8cf677f34	d41ba78d-a9e7-44cb-a060-e5538a51694c	t
1421d183-2492-4394-b963-a4a8cf677f34	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	t
1421d183-2492-4394-b963-a4a8cf677f34	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	t
1421d183-2492-4394-b963-a4a8cf677f34	52fefbc2-756e-4fa1-935f-4596180dc8d0	t
1421d183-2492-4394-b963-a4a8cf677f34	2f43062e-c262-4ef9-879e-3995f334be6c	f
1421d183-2492-4394-b963-a4a8cf677f34	4dae68af-19f6-4918-ae67-6c1fda0827d1	f
1421d183-2492-4394-b963-a4a8cf677f34	ceb503d8-d403-48c1-8c6c-dbb4f929b74b	f
1421d183-2492-4394-b963-a4a8cf677f34	062b2277-882e-4989-9743-97c4485847aa	f
d80bf699-b641-4ea2-9752-42cf143ab825	af49489d-7ba4-47f1-a754-8411691885dd	t
d80bf699-b641-4ea2-9752-42cf143ab825	d41ba78d-a9e7-44cb-a060-e5538a51694c	t
d80bf699-b641-4ea2-9752-42cf143ab825	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	t
d80bf699-b641-4ea2-9752-42cf143ab825	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	t
d80bf699-b641-4ea2-9752-42cf143ab825	52fefbc2-756e-4fa1-935f-4596180dc8d0	t
d80bf699-b641-4ea2-9752-42cf143ab825	2f43062e-c262-4ef9-879e-3995f334be6c	f
d80bf699-b641-4ea2-9752-42cf143ab825	4dae68af-19f6-4918-ae67-6c1fda0827d1	f
d80bf699-b641-4ea2-9752-42cf143ab825	ceb503d8-d403-48c1-8c6c-dbb4f929b74b	f
d80bf699-b641-4ea2-9752-42cf143ab825	062b2277-882e-4989-9743-97c4485847aa	f
6cc96394-08b1-48bc-814e-9ed664c4d09c	93a4cc1a-bddb-4c02-b782-25948d204836	t
6cc96394-08b1-48bc-814e-9ed664c4d09c	68dd7df5-09d7-42d1-8b49-c03b890977c5	t
6cc96394-08b1-48bc-814e-9ed664c4d09c	8ac4ad99-361d-4825-9733-22bc5888b62c	t
6cc96394-08b1-48bc-814e-9ed664c4d09c	50d59a29-3563-47de-98c1-361823d6b5c3	t
6cc96394-08b1-48bc-814e-9ed664c4d09c	ed9a51c3-2280-456c-b474-cc0069829d04	t
6cc96394-08b1-48bc-814e-9ed664c4d09c	350748b2-db1c-4d5c-a1a0-d07f7a72878c	f
6cc96394-08b1-48bc-814e-9ed664c4d09c	53251e92-d6fd-4fc0-b8f2-f95428ca676f	f
6cc96394-08b1-48bc-814e-9ed664c4d09c	3887624b-89fe-4d58-83dc-27e34012554d	f
6cc96394-08b1-48bc-814e-9ed664c4d09c	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	f
da76b89f-97ee-473d-850d-8bb339a8f698	93a4cc1a-bddb-4c02-b782-25948d204836	t
da76b89f-97ee-473d-850d-8bb339a8f698	68dd7df5-09d7-42d1-8b49-c03b890977c5	t
da76b89f-97ee-473d-850d-8bb339a8f698	8ac4ad99-361d-4825-9733-22bc5888b62c	t
da76b89f-97ee-473d-850d-8bb339a8f698	50d59a29-3563-47de-98c1-361823d6b5c3	t
da76b89f-97ee-473d-850d-8bb339a8f698	ed9a51c3-2280-456c-b474-cc0069829d04	t
da76b89f-97ee-473d-850d-8bb339a8f698	350748b2-db1c-4d5c-a1a0-d07f7a72878c	f
da76b89f-97ee-473d-850d-8bb339a8f698	53251e92-d6fd-4fc0-b8f2-f95428ca676f	f
da76b89f-97ee-473d-850d-8bb339a8f698	3887624b-89fe-4d58-83dc-27e34012554d	f
da76b89f-97ee-473d-850d-8bb339a8f698	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	f
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	93a4cc1a-bddb-4c02-b782-25948d204836	t
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	68dd7df5-09d7-42d1-8b49-c03b890977c5	t
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	8ac4ad99-361d-4825-9733-22bc5888b62c	t
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	50d59a29-3563-47de-98c1-361823d6b5c3	t
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	ed9a51c3-2280-456c-b474-cc0069829d04	t
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	350748b2-db1c-4d5c-a1a0-d07f7a72878c	f
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	53251e92-d6fd-4fc0-b8f2-f95428ca676f	f
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	3887624b-89fe-4d58-83dc-27e34012554d	f
eedc3e9e-eac9-4168-b0ca-6e0ab15adf8d	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	f
4788821b-b889-4cd0-9535-74949e39bc37	93a4cc1a-bddb-4c02-b782-25948d204836	t
4788821b-b889-4cd0-9535-74949e39bc37	68dd7df5-09d7-42d1-8b49-c03b890977c5	t
4788821b-b889-4cd0-9535-74949e39bc37	8ac4ad99-361d-4825-9733-22bc5888b62c	t
4788821b-b889-4cd0-9535-74949e39bc37	50d59a29-3563-47de-98c1-361823d6b5c3	t
4788821b-b889-4cd0-9535-74949e39bc37	ed9a51c3-2280-456c-b474-cc0069829d04	t
4788821b-b889-4cd0-9535-74949e39bc37	350748b2-db1c-4d5c-a1a0-d07f7a72878c	f
4788821b-b889-4cd0-9535-74949e39bc37	53251e92-d6fd-4fc0-b8f2-f95428ca676f	f
4788821b-b889-4cd0-9535-74949e39bc37	3887624b-89fe-4d58-83dc-27e34012554d	f
4788821b-b889-4cd0-9535-74949e39bc37	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	f
2ec72663-786b-47f8-9f53-39ce6ff11cbb	93a4cc1a-bddb-4c02-b782-25948d204836	t
2ec72663-786b-47f8-9f53-39ce6ff11cbb	68dd7df5-09d7-42d1-8b49-c03b890977c5	t
2ec72663-786b-47f8-9f53-39ce6ff11cbb	8ac4ad99-361d-4825-9733-22bc5888b62c	t
2ec72663-786b-47f8-9f53-39ce6ff11cbb	50d59a29-3563-47de-98c1-361823d6b5c3	t
2ec72663-786b-47f8-9f53-39ce6ff11cbb	ed9a51c3-2280-456c-b474-cc0069829d04	t
2ec72663-786b-47f8-9f53-39ce6ff11cbb	350748b2-db1c-4d5c-a1a0-d07f7a72878c	f
2ec72663-786b-47f8-9f53-39ce6ff11cbb	53251e92-d6fd-4fc0-b8f2-f95428ca676f	f
2ec72663-786b-47f8-9f53-39ce6ff11cbb	3887624b-89fe-4d58-83dc-27e34012554d	f
2ec72663-786b-47f8-9f53-39ce6ff11cbb	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	f
b35f3c52-1869-42f0-8219-694107c37036	93a4cc1a-bddb-4c02-b782-25948d204836	t
b35f3c52-1869-42f0-8219-694107c37036	68dd7df5-09d7-42d1-8b49-c03b890977c5	t
b35f3c52-1869-42f0-8219-694107c37036	8ac4ad99-361d-4825-9733-22bc5888b62c	t
b35f3c52-1869-42f0-8219-694107c37036	50d59a29-3563-47de-98c1-361823d6b5c3	t
b35f3c52-1869-42f0-8219-694107c37036	ed9a51c3-2280-456c-b474-cc0069829d04	t
b35f3c52-1869-42f0-8219-694107c37036	350748b2-db1c-4d5c-a1a0-d07f7a72878c	f
b35f3c52-1869-42f0-8219-694107c37036	53251e92-d6fd-4fc0-b8f2-f95428ca676f	f
b35f3c52-1869-42f0-8219-694107c37036	3887624b-89fe-4d58-83dc-27e34012554d	f
b35f3c52-1869-42f0-8219-694107c37036	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	f
629e8324-85ac-40be-8940-d6e8ab25eb96	93a4cc1a-bddb-4c02-b782-25948d204836	t
629e8324-85ac-40be-8940-d6e8ab25eb96	68dd7df5-09d7-42d1-8b49-c03b890977c5	t
629e8324-85ac-40be-8940-d6e8ab25eb96	8ac4ad99-361d-4825-9733-22bc5888b62c	t
629e8324-85ac-40be-8940-d6e8ab25eb96	50d59a29-3563-47de-98c1-361823d6b5c3	t
629e8324-85ac-40be-8940-d6e8ab25eb96	ed9a51c3-2280-456c-b474-cc0069829d04	t
629e8324-85ac-40be-8940-d6e8ab25eb96	350748b2-db1c-4d5c-a1a0-d07f7a72878c	f
629e8324-85ac-40be-8940-d6e8ab25eb96	53251e92-d6fd-4fc0-b8f2-f95428ca676f	f
629e8324-85ac-40be-8940-d6e8ab25eb96	3887624b-89fe-4d58-83dc-27e34012554d	f
629e8324-85ac-40be-8940-d6e8ab25eb96	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	f
629e8324-85ac-40be-8940-d6e8ab25eb96	624026aa-d09e-41dc-a080-4dcd0d3af7f2	t
\.


--
-- TOC entry 4137 (class 0 OID 16468)
-- Dependencies: 229
-- Data for Name: client_scope_role_mapping; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_scope_role_mapping (scope_id, role_id) FROM stdin;
2f43062e-c262-4ef9-879e-3995f334be6c	34ad4237-b595-4fc0-9887-56997904fc7f
3887624b-89fe-4d58-83dc-27e34012554d	5763eed4-1479-433d-885c-fe30ee053a9a
\.


--
-- TOC entry 4138 (class 0 OID 16471)
-- Dependencies: 230
-- Data for Name: client_session; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_session (id, client_id, redirect_uri, state, "timestamp", session_id, auth_method, realm_id, auth_user_id, current_action) FROM stdin;
\.


--
-- TOC entry 4139 (class 0 OID 16476)
-- Dependencies: 231
-- Data for Name: client_session_auth_status; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_session_auth_status (authenticator, status, client_session) FROM stdin;
\.


--
-- TOC entry 4140 (class 0 OID 16479)
-- Dependencies: 232
-- Data for Name: client_session_note; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_session_note (name, value, client_session) FROM stdin;
\.


--
-- TOC entry 4141 (class 0 OID 16484)
-- Dependencies: 233
-- Data for Name: client_session_prot_mapper; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_session_prot_mapper (protocol_mapper_id, client_session) FROM stdin;
\.


--
-- TOC entry 4142 (class 0 OID 16487)
-- Dependencies: 234
-- Data for Name: client_session_role; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_session_role (role_id, client_session) FROM stdin;
\.


--
-- TOC entry 4143 (class 0 OID 16490)
-- Dependencies: 235
-- Data for Name: client_user_session_note; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client_user_session_note (name, value, client_session) FROM stdin;
\.


--
-- TOC entry 4144 (class 0 OID 16495)
-- Dependencies: 236
-- Data for Name: component; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) FROM stdin;
e8997755-14a3-4f01-9f57-2a8f74a33915	Trusted Hosts	ae43bcfc-5430-4b91-987e-d6df1d2396aa	trusted-hosts	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	anonymous
d3ed54b8-16d8-464e-a6da-33f046c07938	Consent Required	ae43bcfc-5430-4b91-987e-d6df1d2396aa	consent-required	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	anonymous
2aebac90-12f8-49b1-bce3-222a0fd3adcf	Full Scope Disabled	ae43bcfc-5430-4b91-987e-d6df1d2396aa	scope	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	anonymous
6b1fb1cc-fb7d-4fe1-aa61-745fb995bae4	Max Clients Limit	ae43bcfc-5430-4b91-987e-d6df1d2396aa	max-clients	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	anonymous
b55dbf80-3010-415e-8338-9e0ebb6a461e	Allowed Protocol Mapper Types	ae43bcfc-5430-4b91-987e-d6df1d2396aa	allowed-protocol-mappers	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	anonymous
90d71e8e-7fbd-43a6-aa31-98da3a5870d1	Allowed Client Scopes	ae43bcfc-5430-4b91-987e-d6df1d2396aa	allowed-client-templates	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	anonymous
759b6fb8-3e97-4120-972d-c5a59f9be2e7	Allowed Protocol Mapper Types	ae43bcfc-5430-4b91-987e-d6df1d2396aa	allowed-protocol-mappers	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	authenticated
a6d8cece-d1c4-4702-9d2d-4c9a61676fbc	Allowed Client Scopes	ae43bcfc-5430-4b91-987e-d6df1d2396aa	allowed-client-templates	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	authenticated
a7bc5057-cdbf-4e9f-bddd-56976d1ee507	rsa-generated	ae43bcfc-5430-4b91-987e-d6df1d2396aa	rsa-generated	org.keycloak.keys.KeyProvider	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N
c53cad42-4d79-47bf-9370-aedc28e77626	rsa-enc-generated	ae43bcfc-5430-4b91-987e-d6df1d2396aa	rsa-enc-generated	org.keycloak.keys.KeyProvider	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N
b75ab600-4759-4344-ae28-671003060441	hmac-generated	ae43bcfc-5430-4b91-987e-d6df1d2396aa	hmac-generated	org.keycloak.keys.KeyProvider	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N
06e8d052-33b0-47ba-919c-67de7a97da7b	aes-generated	ae43bcfc-5430-4b91-987e-d6df1d2396aa	aes-generated	org.keycloak.keys.KeyProvider	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N
6e54c146-adcd-4036-84fc-37ac1cd80765	rsa-generated	3b82f5f8-9867-4aa1-a600-ae22c220133a	rsa-generated	org.keycloak.keys.KeyProvider	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N
77c98bf5-085c-49c9-9289-58c5ef73b442	rsa-enc-generated	3b82f5f8-9867-4aa1-a600-ae22c220133a	rsa-enc-generated	org.keycloak.keys.KeyProvider	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N
a99faba7-3ede-43cb-a7bc-f41801e9f995	hmac-generated	3b82f5f8-9867-4aa1-a600-ae22c220133a	hmac-generated	org.keycloak.keys.KeyProvider	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N
c7cfa363-446e-4dc9-9932-41815887ddc8	aes-generated	3b82f5f8-9867-4aa1-a600-ae22c220133a	aes-generated	org.keycloak.keys.KeyProvider	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N
b520bd67-e60c-4ef2-bccb-9c0a2d833cb8	Trusted Hosts	3b82f5f8-9867-4aa1-a600-ae22c220133a	trusted-hosts	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	anonymous
2794989f-5035-46df-943b-da4b044518f9	Consent Required	3b82f5f8-9867-4aa1-a600-ae22c220133a	consent-required	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	anonymous
69517229-80db-4087-92f3-e7e84bc3289f	Full Scope Disabled	3b82f5f8-9867-4aa1-a600-ae22c220133a	scope	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	anonymous
135a425c-9530-474e-b27f-a061d3367ec1	Max Clients Limit	3b82f5f8-9867-4aa1-a600-ae22c220133a	max-clients	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	anonymous
d210dfef-878a-41a2-a49b-971a65383ccb	Allowed Protocol Mapper Types	3b82f5f8-9867-4aa1-a600-ae22c220133a	allowed-protocol-mappers	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	anonymous
d207a407-b794-4db0-af3e-839d323c06ae	Allowed Client Scopes	3b82f5f8-9867-4aa1-a600-ae22c220133a	allowed-client-templates	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	anonymous
896b432f-4491-4340-9501-0b54e81291bc	Allowed Protocol Mapper Types	3b82f5f8-9867-4aa1-a600-ae22c220133a	allowed-protocol-mappers	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	authenticated
b30a23fc-0e4e-4e52-ab67-b7c62b46871a	Allowed Client Scopes	3b82f5f8-9867-4aa1-a600-ae22c220133a	allowed-client-templates	org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	authenticated
5ca001be-a170-43b6-9781-dae63706abe2	\N	3b82f5f8-9867-4aa1-a600-ae22c220133a	declarative-user-profile	org.keycloak.userprofile.UserProfileProvider	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N
\.


--
-- TOC entry 4145 (class 0 OID 16500)
-- Dependencies: 237
-- Data for Name: component_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.component_config (id, component_id, name, value) FROM stdin;
210895c3-db83-4026-92ba-9ffe23c6e59e	759b6fb8-3e97-4120-972d-c5a59f9be2e7	allowed-protocol-mapper-types	oidc-address-mapper
86a873d2-138b-4856-8c52-62ddea3f6e9e	759b6fb8-3e97-4120-972d-c5a59f9be2e7	allowed-protocol-mapper-types	oidc-sha256-pairwise-sub-mapper
63a4fa0c-a88d-4d8e-b857-d72c37b3797a	759b6fb8-3e97-4120-972d-c5a59f9be2e7	allowed-protocol-mapper-types	saml-user-attribute-mapper
749aa9c6-a350-478e-aa9e-c15cbe5d2c0c	759b6fb8-3e97-4120-972d-c5a59f9be2e7	allowed-protocol-mapper-types	saml-user-property-mapper
081e040a-9c54-48f0-a93c-c2355d0d5608	759b6fb8-3e97-4120-972d-c5a59f9be2e7	allowed-protocol-mapper-types	saml-role-list-mapper
401c74bb-e0e3-4432-8954-20ce0b7deb1d	759b6fb8-3e97-4120-972d-c5a59f9be2e7	allowed-protocol-mapper-types	oidc-usermodel-attribute-mapper
6916eeb0-f485-40af-88de-2def239ee689	759b6fb8-3e97-4120-972d-c5a59f9be2e7	allowed-protocol-mapper-types	oidc-full-name-mapper
dc814297-64d9-4ccf-914a-22e01caf4eb7	759b6fb8-3e97-4120-972d-c5a59f9be2e7	allowed-protocol-mapper-types	oidc-usermodel-property-mapper
c910affc-1cdd-4f77-b0b7-123898a499a6	90d71e8e-7fbd-43a6-aa31-98da3a5870d1	allow-default-scopes	true
d47e78e4-4892-4d39-bf57-7cbda8d57467	a6d8cece-d1c4-4702-9d2d-4c9a61676fbc	allow-default-scopes	true
4e390824-cdf4-4ddc-af9f-fc4579dac67a	e8997755-14a3-4f01-9f57-2a8f74a33915	client-uris-must-match	true
d945f885-b0dd-4bfb-a95d-35ad8e638f90	e8997755-14a3-4f01-9f57-2a8f74a33915	host-sending-registration-request-must-match	true
555d8899-b43c-446e-8322-726c97cfd664	6b1fb1cc-fb7d-4fe1-aa61-745fb995bae4	max-clients	200
80f6b16a-e163-4ab6-8f37-61bd58e3bf40	b55dbf80-3010-415e-8338-9e0ebb6a461e	allowed-protocol-mapper-types	saml-user-property-mapper
c161f677-c798-47ce-9383-e36280bdad17	b55dbf80-3010-415e-8338-9e0ebb6a461e	allowed-protocol-mapper-types	oidc-full-name-mapper
1ee7aa0a-404d-40a9-97d1-1f2f91587237	b55dbf80-3010-415e-8338-9e0ebb6a461e	allowed-protocol-mapper-types	saml-role-list-mapper
0ac71a86-abf5-4c75-9e19-3817eb32cab4	b55dbf80-3010-415e-8338-9e0ebb6a461e	allowed-protocol-mapper-types	oidc-address-mapper
7755a2ed-270c-463b-a891-cc3dc30547ad	b55dbf80-3010-415e-8338-9e0ebb6a461e	allowed-protocol-mapper-types	oidc-sha256-pairwise-sub-mapper
1b25f725-af93-48d8-8277-672a4deeefc7	b55dbf80-3010-415e-8338-9e0ebb6a461e	allowed-protocol-mapper-types	saml-user-attribute-mapper
3ac25815-ea0e-4f74-930d-86378d6c4987	b55dbf80-3010-415e-8338-9e0ebb6a461e	allowed-protocol-mapper-types	oidc-usermodel-property-mapper
f881c880-efdf-4437-a8d0-26e6b0ffeab5	b55dbf80-3010-415e-8338-9e0ebb6a461e	allowed-protocol-mapper-types	oidc-usermodel-attribute-mapper
8a80d805-f40a-4a8a-bdf3-bef2a8803d97	b75ab600-4759-4344-ae28-671003060441	priority	100
43ec7e07-984e-4a87-8f6d-533c4fc41c1a	b75ab600-4759-4344-ae28-671003060441	algorithm	HS256
42b0bc7a-cfdb-43be-b270-b8a9fb3bdb5d	b75ab600-4759-4344-ae28-671003060441	kid	059e953f-5b8f-47fe-8ca5-2d97eb79cf17
cf4c7d36-9771-4c6c-9d52-2176ece30a3a	b75ab600-4759-4344-ae28-671003060441	secret	eXZbitwAIW-Ujk-IPftS7ddnzERM7NUNwWpyO2DyHqOokCVbbqjfMeJfEICWHbZLHprKut3_7GJXk5B4A4Hpfg
a37a9cd7-fcba-466d-b24e-9bdc2a93968e	a7bc5057-cdbf-4e9f-bddd-56976d1ee507	keyUse	SIG
9691ddd6-5029-42a5-888b-a650ef291469	a7bc5057-cdbf-4e9f-bddd-56976d1ee507	priority	100
ea88d782-a395-427d-9eb5-04798281e023	a7bc5057-cdbf-4e9f-bddd-56976d1ee507	privateKey	MIIEowIBAAKCAQEA0wXaeGnTDwLWx5EFKoD8dHPNV5Z9byDsTIXeaP8kLCLYB//pL0naYRmWLgSltXcKI6XOxJbnkFNIctItpO0UoIH2g/WovQbtw/BSAEPD02S+l7yMzF2QLIDCCxcW33feJfucChaLKM6g1NZ/kJQGnC2n6knPaha2a1x7f+pVjHJk7xXQCJKVydWBuPoxUaGFBT/AAGT8oOEw6rsJg79Ss3HsqOrSu41h2MRZSfTa393gKyCm+HchfpJfBEThJbHjEU7iZWdt0Cadj0LvatxvdaMGPY9o+PDkmFQRzDg1vbY2mWAsNZFdrFRW+fRnykaxu1kDr1d8gWHlgSR14jjY4QIDAQABAoIBAAb7EpcZKRo+BczkRz94tOFFU22qVMvwUuDVLN5cUl4DaRIarSQbVVYQ+p7bGIBAR92WBpkKRO7JUoQ537is/xmwMu96mPJn4EWzFYNcuoWrt4JdgmrgTXyrZb/oK6GI3Zh0El1jEYdwDnH8BoxJ2U4hVISznGDuD2rNzGgW4G2c677zd/fNQ148zhXJSKkwPs4Hl3UqZ/cTyJFxAaWfwJqd9Eznnjrh9mO9YTLhrPusRSTTGKAcxazNR+DOTHbSzDR1tlGfhdF61kVKYqDd0X7MY38ppk8bLVq8xns2Sx8Lr16zPfMx2Tz3cpu/7N0XQnTP2CsSY0n4/1R7b6FCmAECgYEA+cR5+NL+JSoIHdIg7ZfO4ZrkG+1zRkCFgrJQI82nQJQ0E9A1ZMQBypzMTHAzzUepHBsxKnjFu/2PGnQmOmshn4e60AwHda/bFe9XtVT1JDA0GHQv0rMh7vJTpyHPIJdTGkIZSDqcZK26G7wPOpod0Mfk8Cw7pGiQ1AIWHk28PJECgYEA2Enf+6oZQt4g9CCL/JXRscn0+IfcUwNW/8m1KPFxE7tHa9/NnssfwCAhCBi0yeSTImXeyKLzyIGbv5fkZ8buXwUZ4kAfzI3MCBT6WdHWAu7Ggp8zfK2LXNyKMQslQ8CBhs3+jE//yPUTMlzE/Gsvi3OncaH6+ZbkwI3YAvOiP1ECgYEA6eL8vO8HolGbzCELSLIRvt3GoghXHAArhdnohb580lJ1+d/NQBD0BIGUJjgjqiVizLkB712s/H9SVFlTuvy7PaJiY5QAIEqPBBlerqVh8YNaJVQXvFyWeCVgBG/6f/B0l3AyMJsB/J1aW8JdGGldZjbpwaYKK8jnZkCV6catcKECgYAeyAy/dUrZEGHiPIuPrAbG5bYedL9vdQc5qgxFQm9UXo/gk15hHglrv2bUDygTn5stBxB4v3Awdfmjf8t61X3xOfmgEH42D2wdLlce11U835yS1HNwaAZwddaY+jwYc1o7xC9Y6r29bNuJjFhgkCSZFSGcc3ATe10c5lL6wI+zsQKBgCxo9AUntWJ+GFvIScIRFvd8ejRmh3KGjHWbAR0IbL4HtPDmScqWNNAYQ4iC4oLCibu1cCqD4JgfLPg0KkFvzRID5Uq3rR+qP5FnfXeZAi1/lbmkdiWqIFVmwBmnT9pvZIgQ6yZ7L8k8Iq4KueM6nRDNB7EX7gvE2+gqwEOFVMRO
cd97c42f-331d-4378-80b2-e2808e1f7b82	a7bc5057-cdbf-4e9f-bddd-56976d1ee507	certificate	MIICmzCCAYMCBgGHXBx7FzANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjMwNDA3MTQyNDQzWhcNMzMwNDA3MTQyNjIzWjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDTBdp4adMPAtbHkQUqgPx0c81Xln1vIOxMhd5o/yQsItgH/+kvSdphGZYuBKW1dwojpc7ElueQU0hy0i2k7RSggfaD9ai9Bu3D8FIAQ8PTZL6XvIzMXZAsgMILFxbfd94l+5wKFosozqDU1n+QlAacLafqSc9qFrZrXHt/6lWMcmTvFdAIkpXJ1YG4+jFRoYUFP8AAZPyg4TDquwmDv1Kzceyo6tK7jWHYxFlJ9Nrf3eArIKb4dyF+kl8EROElseMRTuJlZ23QJp2PQu9q3G91owY9j2j48OSYVBHMODW9tjaZYCw1kV2sVFb59GfKRrG7WQOvV3yBYeWBJHXiONjhAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAGQw6p9MDe2AsVg1cWcJSX/BT8g/kz5eqnBlD3nv2P8mN1no22mKqP44QwVHO+r/umHsnxo3SikUaIBC8tXu+ccQuoac0/18BQdbMFryyQu5xegr7FwWxYWg6iYYPEGkmWzTxGwqbGaVJQ2mvCTb3VoNtAxvoIGKn5EGOV44GK4D37Eecv8QasdAvlWiRBdx8pFtmZa6pHhJM6q69n5izglO92Wc20TqpmTw5LB3zkyIvJdRuz9w2Hf+7h7b4z1mYw9Mho+EOquYEwoO2l64PGaPPsX6p4nSfxNZRF3qMADHJACC2NyvzncKUzP/aRC35p/JTWDuBmoAI0l5fb7XFX0=
27bd7cdb-b3be-47f6-a270-0fea80d881b8	06e8d052-33b0-47ba-919c-67de7a97da7b	secret	7UPOxw9SXOynf-LdNTpVlA
fa4823a6-8f36-43a6-bfe5-78b5aa9f4357	06e8d052-33b0-47ba-919c-67de7a97da7b	kid	dd3a7b52-c46a-4a27-89db-3a2446ce6bf8
15aa9c37-d588-45e5-abb6-80b4a6c769e8	06e8d052-33b0-47ba-919c-67de7a97da7b	priority	100
c80036ce-6114-4970-af3c-ad7ee7ed66ea	c53cad42-4d79-47bf-9370-aedc28e77626	privateKey	MIIEpAIBAAKCAQEA32CcaBtCh4JwBpnOCKpkG7tuAXRX96aLGMEovAuciBHvXOduvQrjhvqF9DgQ2gkrXjn0vaK5AOaYFxPingN1jMB4uaLPrvVAcSmFl9ZVohdg7AVH106F0knX2AwpN73cWiUsAEcKksRlqovDf5OPQjCr4gRLbtsxn2O+bRTc97BIfvw3ZuKuymMOy9daMLsvcpTGJf8uOiMcDfgPJs7TBmRQNGuthbfHl2z3QPUKuqljAMU1fUvWAsd992tg1UB7SbJeDfeAHCp46D8pQ9I8jMbum3VSRlzaQwqMg0JDcOJOfsj6FlqHA6eUQWcgtihBG17C9sRP3nkw8koTDs53MQIDAQABAoIBAEV4aGxAPhqiyHBlGRy80VGGoxm0sz8D/rJ+MEr7WX1ABq9gEqKcZlKuS0a+wQ7uVxyA7cqkduD+1kInHw2BiedyXJlvNyP0hkwrW67zK8KzfqMoqUUe2KOj74rzjUYWuBcZBGw5q+IlTnER8oUUwTMWq/4o08TwSqiFfs2x9V7aUZLVIDx4MZDNcqBRgisQzYMCaLIdGtrJo/mqSZhDYKs9WYmqlC69U0DhTgkBGRX9YUFdwrSxNSQi732n2uJzg9iMVo5/Li7qI/Ya/5g/Z91XCj8slgQh8IVdNM0mzBhxAbIyJYVWLMbgIbuyuomoFqf6I16njYKO7yqSDJzQFDkCgYEA8HSd8h1Xl6CsR/bcL36nZTNHwZZrDoeMhgr0XuXwSOwNpL77XQt3c3Zr2Jr3IMQCCm37fSvtDImRMv6kE1ekyc/283ofNvOz2VvviUmChxB6hiUeDoxCrE1VUgUDOQIjKQv9EmeCHP+tCTOoTct1kBe0RosQE8RDN0V33bMRWpcCgYEA7dFcmWuAHM1vSrDbSHsgkadf9k+mcww8a4RhUh7+u1sZI/9yminl3vDKrDjUkJ9Ue+IN+H6JmAuPubgnB20zHTccaqriJkPQsXsXdbwtMT5QyL7NXWQKfw562NivHIdZwmjTZPbEsX6UOI+i3LV13xvsDtYgc1cfK2tXtRvf3XcCgYBb1c41GCvKB12FJq01noPETO+M6iv9Ipvy3eAzFlLNJwqW7zIFeUmn2YSa96SG6RV2ckboqYwhi7De7w4vLPwM79Z7axrc2/rGeyxjHKYze4GwFiECoWMdd3Osnal7bmuLU77V9p5lAOlPaGFRRPGjlMoPXzFGu2CIjYmYLLnmBQKBgQCQqfyELwp9etIqQy8BBjUnpQMQ2B7OeTD/zkVNKSGXYrEGXsudz8LPPPZskR1s5B8GRNpuwLp+TPoe5VCIifq/2NhmYpCy6im27MO2kMOE4v1NQBO4rbRc2bgM1LKgzgzh39ZH4nx/5BnR1j4huBh9oYSU+dg/kU2aoe92cgBZvQKBgQDqf93KAPf3ax7lcAsCC+5H2BN9PAjFpSe4qCGrF5ObSFS25/ZOFs+NbKyyxNS7vPdwouQu3OsQoDdazLkn7uTC0Uwtf4V5YEIYhjgOGIs6QtYgQKMOgEk/CB4PbWMLUB2VG3kHQJUNJjXNduiLvxjGJKZZo07yQujSQNb5NoWxlg==
2fdd9304-8a97-4ea0-936a-7c91cb8e0ccb	c53cad42-4d79-47bf-9370-aedc28e77626	priority	100
57c2dd66-fbce-4efb-bac3-5a34b0b9cba6	c53cad42-4d79-47bf-9370-aedc28e77626	algorithm	RSA-OAEP
5d650fa5-5f06-4d03-b237-6e652e5928bd	c53cad42-4d79-47bf-9370-aedc28e77626	certificate	MIICmzCCAYMCBgGHXBx/7jANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjMwNDA3MTQyNDQ0WhcNMzMwNDA3MTQyNjI0WjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDfYJxoG0KHgnAGmc4IqmQbu24BdFf3posYwSi8C5yIEe9c5269CuOG+oX0OBDaCSteOfS9orkA5pgXE+KeA3WMwHi5os+u9UBxKYWX1lWiF2DsBUfXToXSSdfYDCk3vdxaJSwARwqSxGWqi8N/k49CMKviBEtu2zGfY75tFNz3sEh+/Ddm4q7KYw7L11owuy9ylMYl/y46IxwN+A8mztMGZFA0a62Ft8eXbPdA9Qq6qWMAxTV9S9YCx333a2DVQHtJsl4N94AcKnjoPylD0jyMxu6bdVJGXNpDCoyDQkNw4k5+yPoWWocDp5RBZyC2KEEbXsL2xE/eeTDyShMOzncxAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAFFY8HCMXymUIjNA/kUxZrR4nqwMECm2Ge621CB+8QM1i11IN81Pa/LD8OIDPWPq+1jIWKLmhbKbbThzeBwXmG7m92b1fdrMHE9UZRnM8YdORr4I903nV+PPqd1GUDnOBC4oHMfNfyfmt40wovv9aurWKz2fQX9ufVrVrnGRqL4scjsyXgBJ+JycaE63clrH2rSpST/xfwcsDWrUJQ2biKEzSlbp/msAK4EOMPkUAPhZF4er7UmZoRqABIVQtopjAcCxA9zMQ5FyEjOyQCeliafZg6uh+70G3UBrt4nR1NNq/PK/1AwEW6Yj1cJ8Wl3Ly4bmQnUIQk4vMUrRqrnTAVw=
9156cf47-8c07-4ea4-9088-5d7a39976d8b	c53cad42-4d79-47bf-9370-aedc28e77626	keyUse	ENC
5f85c115-6bf9-4c34-be2b-5e866438d987	77c98bf5-085c-49c9-9289-58c5ef73b442	certificate	MIICpTCCAY0CBgGHqcw0RTANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDDAtBbm5ldHRlRGVtbzAeFw0yMzA0MjIxNjI3MjRaFw0zMzA0MjIxNjI5MDRaMBYxFDASBgNVBAMMC0FubmV0dGVEZW1vMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt2eCLHCa+k+JJnEQSh0moEGp/MOgT16CxF7bGDjKqMHIXDwR+4AVoH1d3xGWKgc5eMtp6cd4LAU6E/j6vVoixUlkqslZxSCAcj9IlMKAEAP3dlhwwolxMihHyBbkxyfSE2RxTc0DwvRjD87Zihjv2K4BQPydOzqx4xjSprDQuJNMg6b7dwNNs0yMaGAvMGexnUZcQfikv8in4KPzz55wRZSeWso1N41sGXTcqFlI3ZVzPx9pl3EpiEvXMknf3ussjLYkR8nvZx8SZEswDyEAqfykJbzq09/t5bfPDfoLaqZApwHt3iDELQX3cZTEiKNX1itSz5A+lYkn0JlIIBjFgwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCxDuyNeXvoh01cfcDpjwG00vHkFQVDFRfslJDl2ayD0LHsxZtFoo4QSiJstlYovhiqiARFM/H+V+7DdkUnsVvFA8TRUZrEYmiaeNiNO6SZbaqp0f8Diu7hJpiHVWwNYykC1sdeqpeo5HDnPTjhzeNDvkC9fNTvA8nrVchqR7TeEcl6dI5eAwOWmqa8UUkztSH2FatmrTVo2RzdPYyKhjgU9ZuFJrV0Yh9sSKxs2t19FZmqm4WNjWyCMhpHK65u1UTLaaVEd3s7PCESppBmOXybtxsfIRORnuPnbORjysUCNztH7KYJs/royh3xyA566/MGWprAY/tLuNVQXQdIJkjM
fc5e7d1b-bf5b-4312-9431-216132b7779f	77c98bf5-085c-49c9-9289-58c5ef73b442	priority	100
dcbfb14f-fd72-45dc-ac5f-6351b6daf2e5	77c98bf5-085c-49c9-9289-58c5ef73b442	algorithm	RSA-OAEP
5642b838-c851-4687-9f5d-3a7c525db016	77c98bf5-085c-49c9-9289-58c5ef73b442	keyUse	ENC
23e0b9d5-ff95-420e-bfbd-c2ebb9771775	77c98bf5-085c-49c9-9289-58c5ef73b442	privateKey	MIIEogIBAAKCAQEAt2eCLHCa+k+JJnEQSh0moEGp/MOgT16CxF7bGDjKqMHIXDwR+4AVoH1d3xGWKgc5eMtp6cd4LAU6E/j6vVoixUlkqslZxSCAcj9IlMKAEAP3dlhwwolxMihHyBbkxyfSE2RxTc0DwvRjD87Zihjv2K4BQPydOzqx4xjSprDQuJNMg6b7dwNNs0yMaGAvMGexnUZcQfikv8in4KPzz55wRZSeWso1N41sGXTcqFlI3ZVzPx9pl3EpiEvXMknf3ussjLYkR8nvZx8SZEswDyEAqfykJbzq09/t5bfPDfoLaqZApwHt3iDELQX3cZTEiKNX1itSz5A+lYkn0JlIIBjFgwIDAQABAoIBAE08npH3hBS5DRGu2MTVfPBNZviHLApwy1JGfi83r4UdQrMxKwOG39S8Jx2ritk9PKNVys9EjSPAucIyfuniPTVnYKkZ7Z+6Pc2fQAJB3OuhuAPTX/1VI2ITm5M0rPkAGJTJHnw8xAJEqDk+i9eAA/X05ZtIJzEL2WFBkWjYmrW3v8WvJCxOHcaBg+9EIASnmnLG7jDr9fdFNriuJs52w5c3qMIqlNSiO6M/a3rrBjBOja3T1ny2YqADWy3UHszxUKNO0SAb9kDEgpzlcnAD+/Srr2vtp5FKsFS4slFD+Aso9z/v2yUl2jkdMr3kBNpo9T7F0R1P3/HEzZgxjOMP6XECgYEA5xRD+saNqVhjl9zvtwyH94LCRJeHGd8o6mT+KlxDZ5ufdO1rOpWCmDLERuuoS3RMsECkkmjEpBEcofcFdVgTqyKh+ilwKM48LHH2l3lEJTqaKtfqEJR7q+CRSH2r8fAiKgto5KeN9KlYxmNr2SVhc2OdL0rzZU/7dLke0QblhlMCgYEAyy8D7gllMOy1l7Gq+gllU3wfnsUYaQr0seAq7sF21QSWKE1HdzEsI2bhuEyYtv9lrHSkAv/citK4W9k1TOn8pN1DQcuHVqThvDnj/02PiV9/vx6vR+mn0Kj5gtlkwuw6r+7nrYUXQwEZ2eWflD6pnCNQVRETqBdsi6Nl/k6KfhECgYBshzwWP8Kw0m6UGJQNaLlDKgqLpI8zO3g//gbRZlvSAWk2xXsjHK0lRlKg7HUyWwDZlsK0cLhKRi8ocpgBsMgsDCv/Dg87WwEv7qZeJfo3cfqOPLIV72bWJraVUVC8ZIfnL0yod3lYe2DSbEduyPun8hUU7SK7ZoE3JOqwSYjrOQKBgHDmbwiVr+S/oyfnUVoeXm9FSUyvuCCmNdGg/josxhsXsVgzmMJiUGRbhpEh/rmwsI5jQb9o7OM2s7pVt4hFfF0flX+52DoM5GHyyP3T2y46IoHQSxVqgBUxz3Ml2Rd2rCWWWqXQjlxtADBTp5h4OUbJWJnsI0ADxAzTfpkQi2QBAoGAJGqqNddCgwt4U84bemuIDXLKLZOdsjjm54nPV26KyveRP42izH6gFbYXSAdYIFYPevPpYPAwYRGKHNqTuabm94hOwPyusdmU061+wQLoN+EItdh1a3zlkgaPUrWtXCe7GfRI4/PY4jnkSbmP9cnS0PXiaTr2awd0I/Rsh2KHm0k=
f78b542a-0833-4f6e-9089-36d3082806e4	6e54c146-adcd-4036-84fc-37ac1cd80765	privateKey	MIIEpAIBAAKCAQEAtpXRhVmahHm1xCLKPhjdUR0SPhm7Dek6C3cmEo/XUBp9vg7/Ibv8G5zBiZeU2QS4uRec121Pyg8mSL7+nmP6U3Vqa8dWU5EEmL1D1p1RSeypJ0blRA0Er+H0CGbdDtcaIurz2w2RZdfDkMt1hwhVmsPS1wF1l+tJCIHrtFn5dcmi53uIsp+//8JdMmth51I4Fv2PcLn8yXWIosvzCqcLT6KeKJxZ4LSDvqBQlWQRydttv2khJCfhoZFUPbKc7SM+l7Qhb91GhAoQCi0WSy5BwlLVe0Jq5NHTneoIND1a5TsVUuCL5u4VE9C3gIjQbd41ugAuC30X41inIhYmzuUiYwIDAQABAoIBAAFJ3GErCTafH0PDlRbMGR+960eJ6fKGr1SuYhPmdPgJP9LsCyvjRjuxpwIAElpXuLABtScmC9aatcP0qRpYDUeyf+5DyV2sm9IyH7zvfkfdxjHTQ8Hvr5VILG80vqGgrfwKGW910I1d8PDgJrOtwODnOeEvjvNkAy9ur0mmCuckYpF2xoetb525aR8UMJNkrjeXR73S0ySZOTiv/v7LYei6m0CYPuUNPCrEpwdSytSA6igVDp2u3MPp0FOK/nQlXW8xIScsda9SkIb7v7fkRNOpbdURAyEYMYB+H/mw0i6nakC0MqO+RVDjg9TnMK8PRcDl9KMsSCyZde6AHvbpUjkCgYEA9CG/gmWV37m4qU5OSixIvgaoyrlpI46ievLOcTRfi1Xdbo5CJHkKjxsLgVkgY4IxQcXSzes8dP5+fLXxfc9hn24rAKJ5rM+T7mtUWJYPaqZbebmep9nT5YAjqF2grFfb7lklyZjQOx3WMfCWGdd86kmEuBu8XMFy4uHqvpU1ILsCgYEAv3YdckMYGtt4xICV8ycbZ6XH/fKBTK4HdcwZW8KyffoJ1P38ZNettYljqMeSeLO9u50IPTnDC+VzkvlS+WZd8eGLJrkfmLfr1goTTUQ194c0zTqrIEA6NbF+KJbLP09TBs5OW2JelGgYkPRmEpvwezlKu1TQUF61l/24BNoMXnkCgYEA052NS5KS32M1VkyyQEkypMVQ+qjNIi05WimuGSK0zyqWzoYxfzwMkw92YaDIGnl4CPNZT3Vg6mjPa3qq4cspa/ErBVnQ7qLgMAKJEmDA7ElXcaQcipKewojYX9EIvVtLIMfVoXH8zcGHMbLB6ZSIiu0/RfeEx0JIO7JosNrVOpECgYArPjA0aHb/Seai6y/y9Tg5VrYu4yDVAtFoh6qKkRTjYx/pXVwuIpB3WgqKSlkIrgACads8iNlGzJIn9ewJrFc3lGtrWZCLW68GbswhPXC68Wy5lbCk3hzHl8kYvcY2DGKCPQJxnWwjNMenpauQNGxCSJRabzOFk36MT5/KaL02cQKBgQDGq1RHXc39LUQTiq0rGF0FNozetFgO0NW0VjG+UT4PrwvhLQIfgIcea/zYdIU/hV9tefkVYrgBTLclQXgbG9sIqNUaUTGjAQQWrixfWmx9sll+ev1NtiWBU6bOiX2kkXOr2TXrSu52kZw9Mv3MvJi8ven4bJTyNzpVaqf4OZsyrA==
79a216c8-c4a8-424e-aef5-6bb417036feb	6e54c146-adcd-4036-84fc-37ac1cd80765	priority	100
914217cf-dd14-4478-83cb-4462bf76bc69	6e54c146-adcd-4036-84fc-37ac1cd80765	certificate	MIICpTCCAY0CBgGHqcwxLzANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDDAtBbm5ldHRlRGVtbzAeFw0yMzA0MjIxNjI3MjRaFw0zMzA0MjIxNjI5MDRaMBYxFDASBgNVBAMMC0FubmV0dGVEZW1vMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtpXRhVmahHm1xCLKPhjdUR0SPhm7Dek6C3cmEo/XUBp9vg7/Ibv8G5zBiZeU2QS4uRec121Pyg8mSL7+nmP6U3Vqa8dWU5EEmL1D1p1RSeypJ0blRA0Er+H0CGbdDtcaIurz2w2RZdfDkMt1hwhVmsPS1wF1l+tJCIHrtFn5dcmi53uIsp+//8JdMmth51I4Fv2PcLn8yXWIosvzCqcLT6KeKJxZ4LSDvqBQlWQRydttv2khJCfhoZFUPbKc7SM+l7Qhb91GhAoQCi0WSy5BwlLVe0Jq5NHTneoIND1a5TsVUuCL5u4VE9C3gIjQbd41ugAuC30X41inIhYmzuUiYwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCJmUsdfCAHQu1IgxrRFhzFm+aV9glxXGtoqFA2G67Rgkm38N1OGhWy0dMKRGMsno8J/WRCbmHEpseAffh9dtz0aFsLPF9kdR9YaSkVTooi2zqueYiwrRc1aYRa0ZzxBtPYtwgR0vJ4qIQRhKWO503WLmAKG9zHfSo7yXfHLOrrS6WUW5GyFL+nW1D7IDs6szWMA2K0HVUOzN6LaV3nSON/0PEfQNCkHf9eehMeDbyXvENnbm04g7HtsCxRCBytWY1S/yig5CLdnJFLVWLdfe3zk2LOqjVliAkq+1SXrrlEJu/Hhg7V2v1x3YbjnjAZi0Kqr3O4dMHzAAWOFVO6tvCs
186cc886-0cad-4bb1-a840-ce89b17dfc1e	6e54c146-adcd-4036-84fc-37ac1cd80765	keyUse	SIG
489e4f89-196a-4b18-ab3f-cbf0f0da3540	a99faba7-3ede-43cb-a7bc-f41801e9f995	priority	100
1bfff676-99d2-49d8-b150-241cbe821b49	a99faba7-3ede-43cb-a7bc-f41801e9f995	kid	ab65ad37-ee0e-4995-96ba-b86940c3db90
c318ab7c-471e-4413-bf80-839d0bb91e74	a99faba7-3ede-43cb-a7bc-f41801e9f995	algorithm	HS256
2b71a827-5b60-4f10-8981-7af7b73ac2f3	a99faba7-3ede-43cb-a7bc-f41801e9f995	secret	MRg1wX8w_o63K65t4fCwetpKEE52Kp0kZnin_IpTuvuez3qir88FGgrEZWCC4UYjsAHNqB1D4_iMPisI4aJCRA
61db0113-1240-4238-9a8a-203140779117	c7cfa363-446e-4dc9-9932-41815887ddc8	priority	100
6c0409be-6b98-467c-9226-141bb40de5f5	c7cfa363-446e-4dc9-9932-41815887ddc8	secret	b9UK6-d13VVQOkpHBkrcNg
f2aa0594-c9e5-402c-a85f-a39ef0749f4e	c7cfa363-446e-4dc9-9932-41815887ddc8	kid	0dcd5a42-989a-46c6-b177-8dacc0249312
2adabd64-11c9-4a56-9435-899fc39f9511	d210dfef-878a-41a2-a49b-971a65383ccb	allowed-protocol-mapper-types	oidc-address-mapper
a3cf8607-c4e2-4be2-b12c-083847c2f645	d210dfef-878a-41a2-a49b-971a65383ccb	allowed-protocol-mapper-types	oidc-full-name-mapper
1b929dbe-696d-45d6-881a-1aca49e019aa	d210dfef-878a-41a2-a49b-971a65383ccb	allowed-protocol-mapper-types	saml-role-list-mapper
144046e6-c3dd-4a80-9403-ea71b86eb3f1	d210dfef-878a-41a2-a49b-971a65383ccb	allowed-protocol-mapper-types	oidc-usermodel-property-mapper
3e35c61f-27da-4fe2-a16d-dc03d00aaa9c	d210dfef-878a-41a2-a49b-971a65383ccb	allowed-protocol-mapper-types	saml-user-attribute-mapper
cae8c74b-536e-4eda-a060-bafc3e45342a	d210dfef-878a-41a2-a49b-971a65383ccb	allowed-protocol-mapper-types	oidc-sha256-pairwise-sub-mapper
30efe865-1d4c-4a29-9b75-0b3105747e64	d210dfef-878a-41a2-a49b-971a65383ccb	allowed-protocol-mapper-types	saml-user-property-mapper
e900cdb0-c46d-4c55-8147-99deedc7d8c6	d210dfef-878a-41a2-a49b-971a65383ccb	allowed-protocol-mapper-types	oidc-usermodel-attribute-mapper
0fd41f63-c686-4500-9f70-7f4c30d967c4	d207a407-b794-4db0-af3e-839d323c06ae	allow-default-scopes	true
3435aa92-afd4-4d8d-bc49-e8237ec6aa5a	b520bd67-e60c-4ef2-bccb-9c0a2d833cb8	host-sending-registration-request-must-match	true
52e2e4e7-937d-42d5-9bbb-9b30fb2b0d86	b520bd67-e60c-4ef2-bccb-9c0a2d833cb8	client-uris-must-match	true
c5a8638c-451f-4680-9cb8-9653039d7f09	b30a23fc-0e4e-4e52-ab67-b7c62b46871a	allow-default-scopes	true
6585349e-a8b3-4ebf-8dab-c425832c2a95	135a425c-9530-474e-b27f-a061d3367ec1	max-clients	200
7a1bb0eb-1290-41cf-a0c7-5151ca306d87	896b432f-4491-4340-9501-0b54e81291bc	allowed-protocol-mapper-types	saml-user-attribute-mapper
65617aee-d5ce-486c-8bf0-929f7b3c9f6c	896b432f-4491-4340-9501-0b54e81291bc	allowed-protocol-mapper-types	oidc-usermodel-property-mapper
159e56e3-1716-421e-bba2-686e6d2f7d15	896b432f-4491-4340-9501-0b54e81291bc	allowed-protocol-mapper-types	oidc-sha256-pairwise-sub-mapper
a46a478c-7deb-45b4-b55e-e353634251b5	896b432f-4491-4340-9501-0b54e81291bc	allowed-protocol-mapper-types	saml-user-property-mapper
252ec02d-0b81-4f74-a5f5-cc29421bd476	896b432f-4491-4340-9501-0b54e81291bc	allowed-protocol-mapper-types	oidc-usermodel-attribute-mapper
7a3f04e8-9464-45ec-af40-2300f59b406f	896b432f-4491-4340-9501-0b54e81291bc	allowed-protocol-mapper-types	saml-role-list-mapper
511bfefe-f37f-451a-b384-6f961eac5d6f	896b432f-4491-4340-9501-0b54e81291bc	allowed-protocol-mapper-types	oidc-address-mapper
23d2bfba-f478-4bf7-b7b9-da33cb80cf17	896b432f-4491-4340-9501-0b54e81291bc	allowed-protocol-mapper-types	oidc-full-name-mapper
\.


--
-- TOC entry 4146 (class 0 OID 16505)
-- Dependencies: 238
-- Data for Name: composite_role; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.composite_role (composite, child_role) FROM stdin;
aef6b1da-0467-4657-892d-885cf1c071bf	9766bcec-df11-44e6-bdb9-b8cef53b6938
aef6b1da-0467-4657-892d-885cf1c071bf	442f8179-a109-472e-9d77-5bbd26e84f6a
aef6b1da-0467-4657-892d-885cf1c071bf	720d7373-823f-4fe1-8d76-400296cf6278
aef6b1da-0467-4657-892d-885cf1c071bf	28d664ca-b3b8-41de-9590-0c68f5f4e3be
aef6b1da-0467-4657-892d-885cf1c071bf	b9f0d66e-0e40-4151-885a-c43bc073c328
aef6b1da-0467-4657-892d-885cf1c071bf	415cebac-5ce5-4cfc-9a24-abd1c8360711
aef6b1da-0467-4657-892d-885cf1c071bf	c414a13b-9b5c-4ca4-a475-5bb3557e7d93
aef6b1da-0467-4657-892d-885cf1c071bf	374ecce0-3cfd-42cd-a4ae-b99a44d3efa1
aef6b1da-0467-4657-892d-885cf1c071bf	3a997451-4a9c-4028-ae20-fffa5aa33572
aef6b1da-0467-4657-892d-885cf1c071bf	595a198a-cb14-4fb4-8db0-48a829ac8047
aef6b1da-0467-4657-892d-885cf1c071bf	59f6e85c-7721-46bc-b805-2a01b210c7cc
aef6b1da-0467-4657-892d-885cf1c071bf	ce649257-3734-41e1-8272-786afe8e80ff
aef6b1da-0467-4657-892d-885cf1c071bf	e9e95071-da01-4732-bbe7-8efc84402609
aef6b1da-0467-4657-892d-885cf1c071bf	2826eb6b-5259-4e86-87a9-66e66f2ea7bf
aef6b1da-0467-4657-892d-885cf1c071bf	743d1d59-404c-4328-a7d5-5df73a12364e
aef6b1da-0467-4657-892d-885cf1c071bf	d9587423-c239-437f-ab08-d461717a13cf
aef6b1da-0467-4657-892d-885cf1c071bf	2284f165-d964-4bb5-95f7-8deb0eec7212
aef6b1da-0467-4657-892d-885cf1c071bf	721c2dfe-1503-4bab-b0e8-2a7dc5215cbe
28d664ca-b3b8-41de-9590-0c68f5f4e3be	743d1d59-404c-4328-a7d5-5df73a12364e
28d664ca-b3b8-41de-9590-0c68f5f4e3be	721c2dfe-1503-4bab-b0e8-2a7dc5215cbe
ad7e0bc1-f0c9-4092-875d-9fc377c138a4	cebf4ba6-7ac1-4630-ba87-e506897ad5bd
b9f0d66e-0e40-4151-885a-c43bc073c328	d9587423-c239-437f-ab08-d461717a13cf
ad7e0bc1-f0c9-4092-875d-9fc377c138a4	ed2cdba8-f1d8-412a-bbcb-8711aaee53b9
ed2cdba8-f1d8-412a-bbcb-8711aaee53b9	009c4c9a-5b7d-4eee-bf43-8681ee58170a
3be8c17c-b04e-4d38-8a41-78553862fa77	d37c80f7-670b-49d6-81a1-cd8237b24fc1
aef6b1da-0467-4657-892d-885cf1c071bf	ba613b55-6c4a-43c5-b5cc-1115cfe02303
ad7e0bc1-f0c9-4092-875d-9fc377c138a4	34ad4237-b595-4fc0-9887-56997904fc7f
ad7e0bc1-f0c9-4092-875d-9fc377c138a4	0873fef2-e29c-430f-be93-4ddecae7769b
aef6b1da-0467-4657-892d-885cf1c071bf	b1b14140-64f1-4fcc-a114-a83d52ca226a
aef6b1da-0467-4657-892d-885cf1c071bf	e8637836-7a03-4a64-b482-509cd6b1fb6a
aef6b1da-0467-4657-892d-885cf1c071bf	07e7d1b8-45a6-48d5-aa6e-797539995264
aef6b1da-0467-4657-892d-885cf1c071bf	8ea2c00b-913d-479c-abee-2362a5e09999
aef6b1da-0467-4657-892d-885cf1c071bf	0b77bc72-6d4c-48f5-8a78-d104dbdad976
aef6b1da-0467-4657-892d-885cf1c071bf	9bf7e0ad-f7c8-40e9-99d4-47b86a27c7a6
aef6b1da-0467-4657-892d-885cf1c071bf	07a8374a-b515-4aad-836d-87d06557e635
aef6b1da-0467-4657-892d-885cf1c071bf	d8d950ef-e465-45a2-973c-d8d31b72882d
aef6b1da-0467-4657-892d-885cf1c071bf	218957ec-bf6b-47ab-be91-89177b97ed9e
aef6b1da-0467-4657-892d-885cf1c071bf	ba4f5aef-a8a6-4ed3-b1c3-d48c31630c7f
aef6b1da-0467-4657-892d-885cf1c071bf	4cd4509c-3f99-4f24-9a58-f60907535092
aef6b1da-0467-4657-892d-885cf1c071bf	caa229c9-131d-4b81-b0ce-5345306ebaa2
aef6b1da-0467-4657-892d-885cf1c071bf	086713d1-88f1-4cf0-845b-bb8fb695d916
aef6b1da-0467-4657-892d-885cf1c071bf	a881b44f-d94d-4e71-9056-11b319d38e01
aef6b1da-0467-4657-892d-885cf1c071bf	132e1eb2-75de-4c1f-908b-17b82de33622
aef6b1da-0467-4657-892d-885cf1c071bf	ba225e8d-39a8-4347-9ace-a679e534173e
aef6b1da-0467-4657-892d-885cf1c071bf	5e46d28d-492a-433b-9b2b-5e731346739c
07e7d1b8-45a6-48d5-aa6e-797539995264	a881b44f-d94d-4e71-9056-11b319d38e01
07e7d1b8-45a6-48d5-aa6e-797539995264	5e46d28d-492a-433b-9b2b-5e731346739c
8ea2c00b-913d-479c-abee-2362a5e09999	132e1eb2-75de-4c1f-908b-17b82de33622
033cca8b-3850-40da-86c1-fb79f9f97f3f	cd5070a5-a3c6-4ad3-accd-5bb3e4d83ea0
033cca8b-3850-40da-86c1-fb79f9f97f3f	1fd28864-d863-4946-9fdd-92eac2c2efdf
033cca8b-3850-40da-86c1-fb79f9f97f3f	5b314a3c-f823-4a35-b98a-7c5846a843a4
033cca8b-3850-40da-86c1-fb79f9f97f3f	54c930c5-2699-4fb8-9fe9-8d60a6d9f6a6
033cca8b-3850-40da-86c1-fb79f9f97f3f	481ebbe7-bd4b-436c-abed-3908116847d4
033cca8b-3850-40da-86c1-fb79f9f97f3f	9d6e2e8e-b79e-4bca-9ac0-ea649bcea284
033cca8b-3850-40da-86c1-fb79f9f97f3f	55d2965c-776b-4c52-bd53-7b4ce7ff2c00
033cca8b-3850-40da-86c1-fb79f9f97f3f	df3247ee-3a1e-412e-9540-4b394c73cac8
033cca8b-3850-40da-86c1-fb79f9f97f3f	3a35e96f-1af7-4d22-9436-e5a28ae30e22
033cca8b-3850-40da-86c1-fb79f9f97f3f	08192651-3d7f-44f8-9d4e-116f13f2bca6
033cca8b-3850-40da-86c1-fb79f9f97f3f	106bbf17-ab80-4755-8612-3bcd27404907
033cca8b-3850-40da-86c1-fb79f9f97f3f	aad0631e-9448-4a67-8ab6-4fa820958c5f
033cca8b-3850-40da-86c1-fb79f9f97f3f	d192f453-df49-44e2-af9a-e1a65cf21779
033cca8b-3850-40da-86c1-fb79f9f97f3f	e9a8a30e-9768-49a4-a625-913d3af746ed
033cca8b-3850-40da-86c1-fb79f9f97f3f	a18010d6-459e-482e-810c-db68805b1ba8
033cca8b-3850-40da-86c1-fb79f9f97f3f	21747962-4038-4e7f-b1d3-4290d9063ed9
033cca8b-3850-40da-86c1-fb79f9f97f3f	7c97080e-6ab1-4dd4-9f4d-97276c98729f
2d27bb5b-0e7d-462c-9089-95a3c08a1559	4e5f7252-77ed-4497-b866-cc39d8adaeb9
54c930c5-2699-4fb8-9fe9-8d60a6d9f6a6	a18010d6-459e-482e-810c-db68805b1ba8
5b314a3c-f823-4a35-b98a-7c5846a843a4	e9a8a30e-9768-49a4-a625-913d3af746ed
5b314a3c-f823-4a35-b98a-7c5846a843a4	7c97080e-6ab1-4dd4-9f4d-97276c98729f
2d27bb5b-0e7d-462c-9089-95a3c08a1559	9f4ea579-5301-48e3-95c6-157e292faad2
9f4ea579-5301-48e3-95c6-157e292faad2	7db418eb-7c63-425a-bcbd-0b287a6ac0c6
6a6eac37-94ac-4d48-9d86-a2d84374309e	57771350-ea81-487c-b0b5-b845314d7ce2
aef6b1da-0467-4657-892d-885cf1c071bf	3a5eb7ea-7466-46c2-88a4-d7d9c44c57fd
033cca8b-3850-40da-86c1-fb79f9f97f3f	a77e4571-a302-4aa6-acf3-0c18fa4b147d
2d27bb5b-0e7d-462c-9089-95a3c08a1559	5763eed4-1479-433d-885c-fe30ee053a9a
2d27bb5b-0e7d-462c-9089-95a3c08a1559	72fd8f57-f75a-4b21-a3bb-afbbce69fd3b
\.


--
-- TOC entry 4147 (class 0 OID 16508)
-- Dependencies: 239
-- Data for Name: credential; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.credential (id, salt, type, user_id, created_date, user_label, secret_data, credential_data, priority) FROM stdin;
73d98ef4-d7c7-429f-9718-292ba23ddc8e	\N	password	ad671e48-45f8-40f9-a807-414c48862820	1682181469020	My password	{"value":"7tPvPa/QsVSU8zyyargXDi79sUOCk+p6oLfBL8EDArVtLT6r35Pdcx9NIcm/0LUhgnLMJG1oOT8sDsURNkfHrw==","salt":"25fyda6e4DDIZXO0M7gcgg==","additionalParameters":{}}	{"hashIterations":27500,"algorithm":"pbkdf2-sha256","additionalParameters":{}}	10
2da3fdd1-af95-4529-a047-a8c4adf1d5ea	\N	password	873d1a17-05e2-4aec-b2d9-89aae8684c43	1684491298634	My password	{"value":"ElXRniLoBRm05TKynPENi5OYpT4W5s3rMqoZg+X+QxSt3eWGuuAa/wMEbLcqm3qQFomd0+UclhyDDBsJeM3UNw==","salt":"YQ7JycbHntOdIw5XWmpvZg==","additionalParameters":{}}	{"hashIterations":27500,"algorithm":"pbkdf2-sha256","additionalParameters":{}}	10
ce28d786-f1f4-469c-9ec1-76b97df13edb	\N	password	d2de545f-d20a-4a4e-9f74-188ef8b1aef1	1680877589719	\N	{"value":"6Bcgo23a60aopR7Xi2NI+yaoPMkTFQXZJK99Namca/8=","salt":"mgal06x11QepdhcZFEzhig==","additionalParameters":{}}	{"hashIterations":27500,"algorithm":"pbkdf2-sha256","additionalParameters":{}}	10
\.


--
-- TOC entry 4148 (class 0 OID 16513)
-- Dependencies: 240
-- Data for Name: databasechangelog; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) FROM stdin;
1.0.0.Final-KEYCLOAK-5461	sthorger@redhat.com	META-INF/jpa-changelog-1.0.0.Final.xml	2023-04-07 14:25:56.212266	1	EXECUTED	8:bda77d94bf90182a1e30c24f1c155ec7	createTable tableName=APPLICATION_DEFAULT_ROLES; createTable tableName=CLIENT; createTable tableName=CLIENT_SESSION; createTable tableName=CLIENT_SESSION_ROLE; createTable tableName=COMPOSITE_ROLE; createTable tableName=CREDENTIAL; createTable tab...		\N	4.8.0	\N	\N	0877552144
1.0.0.Final-KEYCLOAK-5461	sthorger@redhat.com	META-INF/db2-jpa-changelog-1.0.0.Final.xml	2023-04-07 14:25:56.351243	2	MARK_RAN	8:1ecb330f30986693d1cba9ab579fa219	createTable tableName=APPLICATION_DEFAULT_ROLES; createTable tableName=CLIENT; createTable tableName=CLIENT_SESSION; createTable tableName=CLIENT_SESSION_ROLE; createTable tableName=COMPOSITE_ROLE; createTable tableName=CREDENTIAL; createTable tab...		\N	4.8.0	\N	\N	0877552144
1.1.0.Beta1	sthorger@redhat.com	META-INF/jpa-changelog-1.1.0.Beta1.xml	2023-04-07 14:25:56.713298	3	EXECUTED	8:cb7ace19bc6d959f305605d255d4c843	delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=CLIENT_ATTRIBUTES; createTable tableName=CLIENT_SESSION_NOTE; createTable tableName=APP_NODE_REGISTRATIONS; addColumn table...		\N	4.8.0	\N	\N	0877552144
1.1.0.Final	sthorger@redhat.com	META-INF/jpa-changelog-1.1.0.Final.xml	2023-04-07 14:25:56.813589	4	EXECUTED	8:80230013e961310e6872e871be424a63	renameColumn newColumnName=EVENT_TIME, oldColumnName=TIME, tableName=EVENT_ENTITY		\N	4.8.0	\N	\N	0877552144
1.2.0.Beta1	psilva@redhat.com	META-INF/jpa-changelog-1.2.0.Beta1.xml	2023-04-07 14:25:57.297815	5	EXECUTED	8:67f4c20929126adc0c8e9bf48279d244	delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=PROTOCOL_MAPPER; createTable tableName=PROTOCOL_MAPPER_CONFIG; createTable tableName=...		\N	4.8.0	\N	\N	0877552144
1.2.0.Beta1	psilva@redhat.com	META-INF/db2-jpa-changelog-1.2.0.Beta1.xml	2023-04-07 14:25:57.307576	6	MARK_RAN	8:7311018b0b8179ce14628ab412bb6783	delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=PROTOCOL_MAPPER; createTable tableName=PROTOCOL_MAPPER_CONFIG; createTable tableName=...		\N	4.8.0	\N	\N	0877552144
1.2.0.RC1	bburke@redhat.com	META-INF/jpa-changelog-1.2.0.CR1.xml	2023-04-07 14:25:57.893945	7	EXECUTED	8:037ba1216c3640f8785ee6b8e7c8e3c1	delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=MIGRATION_MODEL; createTable tableName=IDENTITY_P...		\N	4.8.0	\N	\N	0877552144
1.2.0.RC1	bburke@redhat.com	META-INF/db2-jpa-changelog-1.2.0.CR1.xml	2023-04-07 14:25:57.921043	8	MARK_RAN	8:7fe6ffe4af4df289b3157de32c624263	delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=MIGRATION_MODEL; createTable tableName=IDENTITY_P...		\N	4.8.0	\N	\N	0877552144
1.2.0.Final	keycloak	META-INF/jpa-changelog-1.2.0.Final.xml	2023-04-07 14:25:57.971205	9	EXECUTED	8:9c136bc3187083a98745c7d03bc8a303	update tableName=CLIENT; update tableName=CLIENT; update tableName=CLIENT		\N	4.8.0	\N	\N	0877552144
1.3.0	bburke@redhat.com	META-INF/jpa-changelog-1.3.0.xml	2023-04-07 14:25:58.343269	10	EXECUTED	8:b5f09474dca81fb56a97cf5b6553d331	delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=ADMI...		\N	4.8.0	\N	\N	0877552144
1.4.0	bburke@redhat.com	META-INF/jpa-changelog-1.4.0.xml	2023-04-07 14:25:58.586211	11	EXECUTED	8:ca924f31bd2a3b219fdcfe78c82dacf4	delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...		\N	4.8.0	\N	\N	0877552144
1.4.0	bburke@redhat.com	META-INF/db2-jpa-changelog-1.4.0.xml	2023-04-07 14:25:58.628832	12	MARK_RAN	8:8acad7483e106416bcfa6f3b824a16cd	delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...		\N	4.8.0	\N	\N	0877552144
1.5.0	bburke@redhat.com	META-INF/jpa-changelog-1.5.0.xml	2023-04-07 14:25:58.761164	13	EXECUTED	8:9b1266d17f4f87c78226f5055408fd5e	delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...		\N	4.8.0	\N	\N	0877552144
1.6.1_from15	mposolda@redhat.com	META-INF/jpa-changelog-1.6.1.xml	2023-04-07 14:25:58.85826	14	EXECUTED	8:d80ec4ab6dbfe573550ff72396c7e910	addColumn tableName=REALM; addColumn tableName=KEYCLOAK_ROLE; addColumn tableName=CLIENT; createTable tableName=OFFLINE_USER_SESSION; createTable tableName=OFFLINE_CLIENT_SESSION; addPrimaryKey constraintName=CONSTRAINT_OFFL_US_SES_PK2, tableName=...		\N	4.8.0	\N	\N	0877552144
1.6.1_from16-pre	mposolda@redhat.com	META-INF/jpa-changelog-1.6.1.xml	2023-04-07 14:25:58.868791	15	MARK_RAN	8:d86eb172171e7c20b9c849b584d147b2	delete tableName=OFFLINE_CLIENT_SESSION; delete tableName=OFFLINE_USER_SESSION		\N	4.8.0	\N	\N	0877552144
1.6.1_from16	mposolda@redhat.com	META-INF/jpa-changelog-1.6.1.xml	2023-04-07 14:25:58.877848	16	MARK_RAN	8:5735f46f0fa60689deb0ecdc2a0dea22	dropPrimaryKey constraintName=CONSTRAINT_OFFLINE_US_SES_PK, tableName=OFFLINE_USER_SESSION; dropPrimaryKey constraintName=CONSTRAINT_OFFLINE_CL_SES_PK, tableName=OFFLINE_CLIENT_SESSION; addColumn tableName=OFFLINE_USER_SESSION; update tableName=OF...		\N	4.8.0	\N	\N	0877552144
1.6.1	mposolda@redhat.com	META-INF/jpa-changelog-1.6.1.xml	2023-04-07 14:25:58.897635	17	EXECUTED	8:d41d8cd98f00b204e9800998ecf8427e	empty		\N	4.8.0	\N	\N	0877552144
1.7.0	bburke@redhat.com	META-INF/jpa-changelog-1.7.0.xml	2023-04-07 14:25:59.109018	18	EXECUTED	8:5c1a8fd2014ac7fc43b90a700f117b23	createTable tableName=KEYCLOAK_GROUP; createTable tableName=GROUP_ROLE_MAPPING; createTable tableName=GROUP_ATTRIBUTE; createTable tableName=USER_GROUP_MEMBERSHIP; createTable tableName=REALM_DEFAULT_GROUPS; addColumn tableName=IDENTITY_PROVIDER; ...		\N	4.8.0	\N	\N	0877552144
1.8.0	mposolda@redhat.com	META-INF/jpa-changelog-1.8.0.xml	2023-04-07 14:25:59.227722	19	EXECUTED	8:1f6c2c2dfc362aff4ed75b3f0ef6b331	addColumn tableName=IDENTITY_PROVIDER; createTable tableName=CLIENT_TEMPLATE; createTable tableName=CLIENT_TEMPLATE_ATTRIBUTES; createTable tableName=TEMPLATE_SCOPE_MAPPING; dropNotNullConstraint columnName=CLIENT_ID, tableName=PROTOCOL_MAPPER; ad...		\N	4.8.0	\N	\N	0877552144
1.8.0-2	keycloak	META-INF/jpa-changelog-1.8.0.xml	2023-04-07 14:25:59.233257	20	EXECUTED	8:dee9246280915712591f83a127665107	dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; update tableName=CREDENTIAL		\N	4.8.0	\N	\N	0877552144
authz-3.4.0.CR1-resource-server-pk-change-part1	glavoie@gmail.com	META-INF/jpa-changelog-authz-3.4.0.CR1.xml	2023-04-07 14:26:00.813141	45	EXECUTED	8:a164ae073c56ffdbc98a615493609a52	addColumn tableName=RESOURCE_SERVER_POLICY; addColumn tableName=RESOURCE_SERVER_RESOURCE; addColumn tableName=RESOURCE_SERVER_SCOPE		\N	4.8.0	\N	\N	0877552144
1.8.0	mposolda@redhat.com	META-INF/db2-jpa-changelog-1.8.0.xml	2023-04-07 14:25:59.244698	21	MARK_RAN	8:9eb2ee1fa8ad1c5e426421a6f8fdfa6a	addColumn tableName=IDENTITY_PROVIDER; createTable tableName=CLIENT_TEMPLATE; createTable tableName=CLIENT_TEMPLATE_ATTRIBUTES; createTable tableName=TEMPLATE_SCOPE_MAPPING; dropNotNullConstraint columnName=CLIENT_ID, tableName=PROTOCOL_MAPPER; ad...		\N	4.8.0	\N	\N	0877552144
1.8.0-2	keycloak	META-INF/db2-jpa-changelog-1.8.0.xml	2023-04-07 14:25:59.24893	22	MARK_RAN	8:dee9246280915712591f83a127665107	dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; update tableName=CREDENTIAL		\N	4.8.0	\N	\N	0877552144
1.9.0	mposolda@redhat.com	META-INF/jpa-changelog-1.9.0.xml	2023-04-07 14:25:59.327694	23	EXECUTED	8:d9fa18ffa355320395b86270680dd4fe	update tableName=REALM; update tableName=REALM; update tableName=REALM; update tableName=REALM; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=REALM; update tableName=REALM; customChange; dr...		\N	4.8.0	\N	\N	0877552144
1.9.1	keycloak	META-INF/jpa-changelog-1.9.1.xml	2023-04-07 14:25:59.341752	24	EXECUTED	8:90cff506fedb06141ffc1c71c4a1214c	modifyDataType columnName=PRIVATE_KEY, tableName=REALM; modifyDataType columnName=PUBLIC_KEY, tableName=REALM; modifyDataType columnName=CERTIFICATE, tableName=REALM		\N	4.8.0	\N	\N	0877552144
1.9.1	keycloak	META-INF/db2-jpa-changelog-1.9.1.xml	2023-04-07 14:25:59.348113	25	MARK_RAN	8:11a788aed4961d6d29c427c063af828c	modifyDataType columnName=PRIVATE_KEY, tableName=REALM; modifyDataType columnName=CERTIFICATE, tableName=REALM		\N	4.8.0	\N	\N	0877552144
1.9.2	keycloak	META-INF/jpa-changelog-1.9.2.xml	2023-04-07 14:25:59.384506	26	EXECUTED	8:a4218e51e1faf380518cce2af5d39b43	createIndex indexName=IDX_USER_EMAIL, tableName=USER_ENTITY; createIndex indexName=IDX_USER_ROLE_MAPPING, tableName=USER_ROLE_MAPPING; createIndex indexName=IDX_USER_GROUP_MAPPING, tableName=USER_GROUP_MEMBERSHIP; createIndex indexName=IDX_USER_CO...		\N	4.8.0	\N	\N	0877552144
authz-2.0.0	psilva@redhat.com	META-INF/jpa-changelog-authz-2.0.0.xml	2023-04-07 14:25:59.638745	27	EXECUTED	8:d9e9a1bfaa644da9952456050f07bbdc	createTable tableName=RESOURCE_SERVER; addPrimaryKey constraintName=CONSTRAINT_FARS, tableName=RESOURCE_SERVER; addUniqueConstraint constraintName=UK_AU8TT6T700S9V50BU18WS5HA6, tableName=RESOURCE_SERVER; createTable tableName=RESOURCE_SERVER_RESOU...		\N	4.8.0	\N	\N	0877552144
authz-2.5.1	psilva@redhat.com	META-INF/jpa-changelog-authz-2.5.1.xml	2023-04-07 14:25:59.666002	28	EXECUTED	8:d1bf991a6163c0acbfe664b615314505	update tableName=RESOURCE_SERVER_POLICY		\N	4.8.0	\N	\N	0877552144
2.1.0-KEYCLOAK-5461	bburke@redhat.com	META-INF/jpa-changelog-2.1.0.xml	2023-04-07 14:25:59.861738	29	EXECUTED	8:88a743a1e87ec5e30bf603da68058a8c	createTable tableName=BROKER_LINK; createTable tableName=FED_USER_ATTRIBUTE; createTable tableName=FED_USER_CONSENT; createTable tableName=FED_USER_CONSENT_ROLE; createTable tableName=FED_USER_CONSENT_PROT_MAPPER; createTable tableName=FED_USER_CR...		\N	4.8.0	\N	\N	0877552144
2.2.0	bburke@redhat.com	META-INF/jpa-changelog-2.2.0.xml	2023-04-07 14:25:59.901515	30	EXECUTED	8:c5517863c875d325dea463d00ec26d7a	addColumn tableName=ADMIN_EVENT_ENTITY; createTable tableName=CREDENTIAL_ATTRIBUTE; createTable tableName=FED_CREDENTIAL_ATTRIBUTE; modifyDataType columnName=VALUE, tableName=CREDENTIAL; addForeignKeyConstraint baseTableName=FED_CREDENTIAL_ATTRIBU...		\N	4.8.0	\N	\N	0877552144
2.3.0	bburke@redhat.com	META-INF/jpa-changelog-2.3.0.xml	2023-04-07 14:25:59.979402	31	EXECUTED	8:ada8b4833b74a498f376d7136bc7d327	createTable tableName=FEDERATED_USER; addPrimaryKey constraintName=CONSTR_FEDERATED_USER, tableName=FEDERATED_USER; dropDefaultValue columnName=TOTP, tableName=USER_ENTITY; dropColumn columnName=TOTP, tableName=USER_ENTITY; addColumn tableName=IDE...		\N	4.8.0	\N	\N	0877552144
2.4.0	bburke@redhat.com	META-INF/jpa-changelog-2.4.0.xml	2023-04-07 14:25:59.992717	32	EXECUTED	8:b9b73c8ea7299457f99fcbb825c263ba	customChange		\N	4.8.0	\N	\N	0877552144
2.5.0	bburke@redhat.com	META-INF/jpa-changelog-2.5.0.xml	2023-04-07 14:26:00.018298	33	EXECUTED	8:07724333e625ccfcfc5adc63d57314f3	customChange; modifyDataType columnName=USER_ID, tableName=OFFLINE_USER_SESSION		\N	4.8.0	\N	\N	0877552144
2.5.0-unicode-oracle	hmlnarik@redhat.com	META-INF/jpa-changelog-2.5.0.xml	2023-04-07 14:26:00.032076	34	MARK_RAN	8:8b6fd445958882efe55deb26fc541a7b	modifyDataType columnName=DESCRIPTION, tableName=AUTHENTICATION_FLOW; modifyDataType columnName=DESCRIPTION, tableName=CLIENT_TEMPLATE; modifyDataType columnName=DESCRIPTION, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=DESCRIPTION,...		\N	4.8.0	\N	\N	0877552144
2.5.0-unicode-other-dbs	hmlnarik@redhat.com	META-INF/jpa-changelog-2.5.0.xml	2023-04-07 14:26:00.208201	35	EXECUTED	8:29b29cfebfd12600897680147277a9d7	modifyDataType columnName=DESCRIPTION, tableName=AUTHENTICATION_FLOW; modifyDataType columnName=DESCRIPTION, tableName=CLIENT_TEMPLATE; modifyDataType columnName=DESCRIPTION, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=DESCRIPTION,...		\N	4.8.0	\N	\N	0877552144
2.5.0-duplicate-email-support	slawomir@dabek.name	META-INF/jpa-changelog-2.5.0.xml	2023-04-07 14:26:00.241124	36	EXECUTED	8:73ad77ca8fd0410c7f9f15a471fa52bc	addColumn tableName=REALM		\N	4.8.0	\N	\N	0877552144
2.5.0-unique-group-names	hmlnarik@redhat.com	META-INF/jpa-changelog-2.5.0.xml	2023-04-07 14:26:00.253308	37	EXECUTED	8:64f27a6fdcad57f6f9153210f2ec1bdb	addUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP		\N	4.8.0	\N	\N	0877552144
2.5.1	bburke@redhat.com	META-INF/jpa-changelog-2.5.1.xml	2023-04-07 14:26:00.279178	38	EXECUTED	8:27180251182e6c31846c2ddab4bc5781	addColumn tableName=FED_USER_CONSENT		\N	4.8.0	\N	\N	0877552144
3.0.0	bburke@redhat.com	META-INF/jpa-changelog-3.0.0.xml	2023-04-07 14:26:00.311354	39	EXECUTED	8:d56f201bfcfa7a1413eb3e9bc02978f9	addColumn tableName=IDENTITY_PROVIDER		\N	4.8.0	\N	\N	0877552144
3.2.0-fix	keycloak	META-INF/jpa-changelog-3.2.0.xml	2023-04-07 14:26:00.315173	40	MARK_RAN	8:91f5522bf6afdc2077dfab57fbd3455c	addNotNullConstraint columnName=REALM_ID, tableName=CLIENT_INITIAL_ACCESS		\N	4.8.0	\N	\N	0877552144
3.2.0-fix-with-keycloak-5416	keycloak	META-INF/jpa-changelog-3.2.0.xml	2023-04-07 14:26:00.326382	41	MARK_RAN	8:0f01b554f256c22caeb7d8aee3a1cdc8	dropIndex indexName=IDX_CLIENT_INIT_ACC_REALM, tableName=CLIENT_INITIAL_ACCESS; addNotNullConstraint columnName=REALM_ID, tableName=CLIENT_INITIAL_ACCESS; createIndex indexName=IDX_CLIENT_INIT_ACC_REALM, tableName=CLIENT_INITIAL_ACCESS		\N	4.8.0	\N	\N	0877552144
3.2.0-fix-offline-sessions	hmlnarik	META-INF/jpa-changelog-3.2.0.xml	2023-04-07 14:26:00.361427	42	EXECUTED	8:ab91cf9cee415867ade0e2df9651a947	customChange		\N	4.8.0	\N	\N	0877552144
3.2.0-fixed	keycloak	META-INF/jpa-changelog-3.2.0.xml	2023-04-07 14:26:00.765204	43	EXECUTED	8:ceac9b1889e97d602caf373eadb0d4b7	addColumn tableName=REALM; dropPrimaryKey constraintName=CONSTRAINT_OFFL_CL_SES_PK2, tableName=OFFLINE_CLIENT_SESSION; dropColumn columnName=CLIENT_SESSION_ID, tableName=OFFLINE_CLIENT_SESSION; addPrimaryKey constraintName=CONSTRAINT_OFFL_CL_SES_P...		\N	4.8.0	\N	\N	0877552144
3.3.0	keycloak	META-INF/jpa-changelog-3.3.0.xml	2023-04-07 14:26:00.795629	44	EXECUTED	8:84b986e628fe8f7fd8fd3c275c5259f2	addColumn tableName=USER_ENTITY		\N	4.8.0	\N	\N	0877552144
authz-3.4.0.CR1-resource-server-pk-change-part2-KEYCLOAK-6095	hmlnarik@redhat.com	META-INF/jpa-changelog-authz-3.4.0.CR1.xml	2023-04-07 14:26:00.832899	46	EXECUTED	8:70a2b4f1f4bd4dbf487114bdb1810e64	customChange		\N	4.8.0	\N	\N	0877552144
authz-3.4.0.CR1-resource-server-pk-change-part3-fixed	glavoie@gmail.com	META-INF/jpa-changelog-authz-3.4.0.CR1.xml	2023-04-07 14:26:00.834922	47	MARK_RAN	8:7be68b71d2f5b94b8df2e824f2860fa2	dropIndex indexName=IDX_RES_SERV_POL_RES_SERV, tableName=RESOURCE_SERVER_POLICY; dropIndex indexName=IDX_RES_SRV_RES_RES_SRV, tableName=RESOURCE_SERVER_RESOURCE; dropIndex indexName=IDX_RES_SRV_SCOPE_RES_SRV, tableName=RESOURCE_SERVER_SCOPE		\N	4.8.0	\N	\N	0877552144
authz-3.4.0.CR1-resource-server-pk-change-part3-fixed-nodropindex	glavoie@gmail.com	META-INF/jpa-changelog-authz-3.4.0.CR1.xml	2023-04-07 14:26:01.015982	48	EXECUTED	8:bab7c631093c3861d6cf6144cd944982	addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, tableName=RESOURCE_SERVER_POLICY; addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, tableName=RESOURCE_SERVER_RESOURCE; addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, ...		\N	4.8.0	\N	\N	0877552144
authn-3.4.0.CR1-refresh-token-max-reuse	glavoie@gmail.com	META-INF/jpa-changelog-authz-3.4.0.CR1.xml	2023-04-07 14:26:01.051035	49	EXECUTED	8:fa809ac11877d74d76fe40869916daad	addColumn tableName=REALM		\N	4.8.0	\N	\N	0877552144
3.4.0	keycloak	META-INF/jpa-changelog-3.4.0.xml	2023-04-07 14:26:01.190527	50	EXECUTED	8:fac23540a40208f5f5e326f6ceb4d291	addPrimaryKey constraintName=CONSTRAINT_REALM_DEFAULT_ROLES, tableName=REALM_DEFAULT_ROLES; addPrimaryKey constraintName=CONSTRAINT_COMPOSITE_ROLE, tableName=COMPOSITE_ROLE; addPrimaryKey constraintName=CONSTR_REALM_DEFAULT_GROUPS, tableName=REALM...		\N	4.8.0	\N	\N	0877552144
3.4.0-KEYCLOAK-5230	hmlnarik@redhat.com	META-INF/jpa-changelog-3.4.0.xml	2023-04-07 14:26:01.288563	51	EXECUTED	8:2612d1b8a97e2b5588c346e817307593	createIndex indexName=IDX_FU_ATTRIBUTE, tableName=FED_USER_ATTRIBUTE; createIndex indexName=IDX_FU_CONSENT, tableName=FED_USER_CONSENT; createIndex indexName=IDX_FU_CONSENT_RU, tableName=FED_USER_CONSENT; createIndex indexName=IDX_FU_CREDENTIAL, t...		\N	4.8.0	\N	\N	0877552144
3.4.1	psilva@redhat.com	META-INF/jpa-changelog-3.4.1.xml	2023-04-07 14:26:01.306607	52	EXECUTED	8:9842f155c5db2206c88bcb5d1046e941	modifyDataType columnName=VALUE, tableName=CLIENT_ATTRIBUTES		\N	4.8.0	\N	\N	0877552144
3.4.2	keycloak	META-INF/jpa-changelog-3.4.2.xml	2023-04-07 14:26:01.327003	53	EXECUTED	8:2e12e06e45498406db72d5b3da5bbc76	update tableName=REALM		\N	4.8.0	\N	\N	0877552144
3.4.2-KEYCLOAK-5172	mkanis@redhat.com	META-INF/jpa-changelog-3.4.2.xml	2023-04-07 14:26:01.352769	54	EXECUTED	8:33560e7c7989250c40da3abdabdc75a4	update tableName=CLIENT		\N	4.8.0	\N	\N	0877552144
4.0.0-KEYCLOAK-6335	bburke@redhat.com	META-INF/jpa-changelog-4.0.0.xml	2023-04-07 14:26:01.395	55	EXECUTED	8:87a8d8542046817a9107c7eb9cbad1cd	createTable tableName=CLIENT_AUTH_FLOW_BINDINGS; addPrimaryKey constraintName=C_CLI_FLOW_BIND, tableName=CLIENT_AUTH_FLOW_BINDINGS		\N	4.8.0	\N	\N	0877552144
4.0.0-CLEANUP-UNUSED-TABLE	bburke@redhat.com	META-INF/jpa-changelog-4.0.0.xml	2023-04-07 14:26:01.418235	56	EXECUTED	8:3ea08490a70215ed0088c273d776311e	dropTable tableName=CLIENT_IDENTITY_PROV_MAPPING		\N	4.8.0	\N	\N	0877552144
4.0.0-KEYCLOAK-6228	bburke@redhat.com	META-INF/jpa-changelog-4.0.0.xml	2023-04-07 14:26:01.519354	57	EXECUTED	8:2d56697c8723d4592ab608ce14b6ed68	dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; dropNotNullConstraint columnName=CLIENT_ID, tableName=USER_CONSENT; addColumn tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHO...		\N	4.8.0	\N	\N	0877552144
4.0.0-KEYCLOAK-5579-fixed	mposolda@redhat.com	META-INF/jpa-changelog-4.0.0.xml	2023-04-07 14:26:01.976626	58	EXECUTED	8:3e423e249f6068ea2bbe48bf907f9d86	dropForeignKeyConstraint baseTableName=CLIENT_TEMPLATE_ATTRIBUTES, constraintName=FK_CL_TEMPL_ATTR_TEMPL; renameTable newTableName=CLIENT_SCOPE_ATTRIBUTES, oldTableName=CLIENT_TEMPLATE_ATTRIBUTES; renameColumn newColumnName=SCOPE_ID, oldColumnName...		\N	4.8.0	\N	\N	0877552144
authz-4.0.0.CR1	psilva@redhat.com	META-INF/jpa-changelog-authz-4.0.0.CR1.xml	2023-04-07 14:26:02.11471	59	EXECUTED	8:15cabee5e5df0ff099510a0fc03e4103	createTable tableName=RESOURCE_SERVER_PERM_TICKET; addPrimaryKey constraintName=CONSTRAINT_FAPMT, tableName=RESOURCE_SERVER_PERM_TICKET; addForeignKeyConstraint baseTableName=RESOURCE_SERVER_PERM_TICKET, constraintName=FK_FRSRHO213XCX4WNKOG82SSPMT...		\N	4.8.0	\N	\N	0877552144
authz-4.0.0.Beta3	psilva@redhat.com	META-INF/jpa-changelog-authz-4.0.0.Beta3.xml	2023-04-07 14:26:02.140973	60	EXECUTED	8:4b80200af916ac54d2ffbfc47918ab0e	addColumn tableName=RESOURCE_SERVER_POLICY; addColumn tableName=RESOURCE_SERVER_PERM_TICKET; addForeignKeyConstraint baseTableName=RESOURCE_SERVER_PERM_TICKET, constraintName=FK_FRSRPO2128CX4WNKOG82SSRFY, referencedTableName=RESOURCE_SERVER_POLICY		\N	4.8.0	\N	\N	0877552144
authz-4.2.0.Final	mhajas@redhat.com	META-INF/jpa-changelog-authz-4.2.0.Final.xml	2023-04-07 14:26:02.194366	61	EXECUTED	8:66564cd5e168045d52252c5027485bbb	createTable tableName=RESOURCE_URIS; addForeignKeyConstraint baseTableName=RESOURCE_URIS, constraintName=FK_RESOURCE_SERVER_URIS, referencedTableName=RESOURCE_SERVER_RESOURCE; customChange; dropColumn columnName=URI, tableName=RESOURCE_SERVER_RESO...		\N	4.8.0	\N	\N	0877552144
authz-4.2.0.Final-KEYCLOAK-9944	hmlnarik@redhat.com	META-INF/jpa-changelog-authz-4.2.0.Final.xml	2023-04-07 14:26:02.211735	62	EXECUTED	8:1c7064fafb030222be2bd16ccf690f6f	addPrimaryKey constraintName=CONSTRAINT_RESOUR_URIS_PK, tableName=RESOURCE_URIS		\N	4.8.0	\N	\N	0877552144
4.2.0-KEYCLOAK-6313	wadahiro@gmail.com	META-INF/jpa-changelog-4.2.0.xml	2023-04-07 14:26:02.228107	63	EXECUTED	8:2de18a0dce10cdda5c7e65c9b719b6e5	addColumn tableName=REQUIRED_ACTION_PROVIDER		\N	4.8.0	\N	\N	0877552144
4.3.0-KEYCLOAK-7984	wadahiro@gmail.com	META-INF/jpa-changelog-4.3.0.xml	2023-04-07 14:26:02.23636	64	EXECUTED	8:03e413dd182dcbd5c57e41c34d0ef682	update tableName=REQUIRED_ACTION_PROVIDER		\N	4.8.0	\N	\N	0877552144
4.6.0-KEYCLOAK-7950	psilva@redhat.com	META-INF/jpa-changelog-4.6.0.xml	2023-04-07 14:26:02.317877	65	EXECUTED	8:d27b42bb2571c18fbe3fe4e4fb7582a7	update tableName=RESOURCE_SERVER_RESOURCE		\N	4.8.0	\N	\N	0877552144
4.6.0-KEYCLOAK-8377	keycloak	META-INF/jpa-changelog-4.6.0.xml	2023-04-07 14:26:02.413587	66	EXECUTED	8:698baf84d9fd0027e9192717c2154fb8	createTable tableName=ROLE_ATTRIBUTE; addPrimaryKey constraintName=CONSTRAINT_ROLE_ATTRIBUTE_PK, tableName=ROLE_ATTRIBUTE; addForeignKeyConstraint baseTableName=ROLE_ATTRIBUTE, constraintName=FK_ROLE_ATTRIBUTE_ID, referencedTableName=KEYCLOAK_ROLE...		\N	4.8.0	\N	\N	0877552144
4.6.0-KEYCLOAK-8555	gideonray@gmail.com	META-INF/jpa-changelog-4.6.0.xml	2023-04-07 14:26:02.458575	67	EXECUTED	8:ced8822edf0f75ef26eb51582f9a821a	createIndex indexName=IDX_COMPONENT_PROVIDER_TYPE, tableName=COMPONENT		\N	4.8.0	\N	\N	0877552144
4.7.0-KEYCLOAK-1267	sguilhen@redhat.com	META-INF/jpa-changelog-4.7.0.xml	2023-04-07 14:26:02.515705	68	EXECUTED	8:f0abba004cf429e8afc43056df06487d	addColumn tableName=REALM		\N	4.8.0	\N	\N	0877552144
4.7.0-KEYCLOAK-7275	keycloak	META-INF/jpa-changelog-4.7.0.xml	2023-04-07 14:26:02.606183	69	EXECUTED	8:6662f8b0b611caa359fcf13bf63b4e24	renameColumn newColumnName=CREATED_ON, oldColumnName=LAST_SESSION_REFRESH, tableName=OFFLINE_USER_SESSION; addNotNullConstraint columnName=CREATED_ON, tableName=OFFLINE_USER_SESSION; addColumn tableName=OFFLINE_USER_SESSION; customChange; createIn...		\N	4.8.0	\N	\N	0877552144
4.8.0-KEYCLOAK-8835	sguilhen@redhat.com	META-INF/jpa-changelog-4.8.0.xml	2023-04-07 14:26:02.668988	70	EXECUTED	8:9e6b8009560f684250bdbdf97670d39e	addNotNullConstraint columnName=SSO_MAX_LIFESPAN_REMEMBER_ME, tableName=REALM; addNotNullConstraint columnName=SSO_IDLE_TIMEOUT_REMEMBER_ME, tableName=REALM		\N	4.8.0	\N	\N	0877552144
authz-7.0.0-KEYCLOAK-10443	psilva@redhat.com	META-INF/jpa-changelog-authz-7.0.0.xml	2023-04-07 14:26:02.696684	71	EXECUTED	8:4223f561f3b8dc655846562b57bb502e	addColumn tableName=RESOURCE_SERVER		\N	4.8.0	\N	\N	0877552144
8.0.0-adding-credential-columns	keycloak	META-INF/jpa-changelog-8.0.0.xml	2023-04-07 14:26:02.745203	72	EXECUTED	8:215a31c398b363ce383a2b301202f29e	addColumn tableName=CREDENTIAL; addColumn tableName=FED_USER_CREDENTIAL		\N	4.8.0	\N	\N	0877552144
8.0.0-updating-credential-data-not-oracle-fixed	keycloak	META-INF/jpa-changelog-8.0.0.xml	2023-04-07 14:26:02.789006	73	EXECUTED	8:83f7a671792ca98b3cbd3a1a34862d3d	update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL		\N	4.8.0	\N	\N	0877552144
8.0.0-updating-credential-data-oracle-fixed	keycloak	META-INF/jpa-changelog-8.0.0.xml	2023-04-07 14:26:02.792133	74	MARK_RAN	8:f58ad148698cf30707a6efbdf8061aa7	update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL		\N	4.8.0	\N	\N	0877552144
8.0.0-credential-cleanup-fixed	keycloak	META-INF/jpa-changelog-8.0.0.xml	2023-04-07 14:26:02.866465	75	EXECUTED	8:79e4fd6c6442980e58d52ffc3ee7b19c	dropDefaultValue columnName=COUNTER, tableName=CREDENTIAL; dropDefaultValue columnName=DIGITS, tableName=CREDENTIAL; dropDefaultValue columnName=PERIOD, tableName=CREDENTIAL; dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; dropColumn ...		\N	4.8.0	\N	\N	0877552144
8.0.0-resource-tag-support	keycloak	META-INF/jpa-changelog-8.0.0.xml	2023-04-07 14:26:02.896509	76	EXECUTED	8:87af6a1e6d241ca4b15801d1f86a297d	addColumn tableName=MIGRATION_MODEL; createIndex indexName=IDX_UPDATE_TIME, tableName=MIGRATION_MODEL		\N	4.8.0	\N	\N	0877552144
9.0.0-always-display-client	keycloak	META-INF/jpa-changelog-9.0.0.xml	2023-04-07 14:26:02.926336	77	EXECUTED	8:b44f8d9b7b6ea455305a6d72a200ed15	addColumn tableName=CLIENT		\N	4.8.0	\N	\N	0877552144
9.0.0-drop-constraints-for-column-increase	keycloak	META-INF/jpa-changelog-9.0.0.xml	2023-04-07 14:26:02.937762	78	MARK_RAN	8:2d8ed5aaaeffd0cb004c046b4a903ac5	dropUniqueConstraint constraintName=UK_FRSR6T700S9V50BU18WS5PMT, tableName=RESOURCE_SERVER_PERM_TICKET; dropUniqueConstraint constraintName=UK_FRSR6T700S9V50BU18WS5HA6, tableName=RESOURCE_SERVER_RESOURCE; dropPrimaryKey constraintName=CONSTRAINT_O...		\N	4.8.0	\N	\N	0877552144
9.0.0-increase-column-size-federated-fk	keycloak	META-INF/jpa-changelog-9.0.0.xml	2023-04-07 14:26:03.027036	79	EXECUTED	8:e290c01fcbc275326c511633f6e2acde	modifyDataType columnName=CLIENT_ID, tableName=FED_USER_CONSENT; modifyDataType columnName=CLIENT_REALM_CONSTRAINT, tableName=KEYCLOAK_ROLE; modifyDataType columnName=OWNER, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=CLIENT_ID, ta...		\N	4.8.0	\N	\N	0877552144
9.0.0-recreate-constraints-after-column-increase	keycloak	META-INF/jpa-changelog-9.0.0.xml	2023-04-07 14:26:03.045114	80	MARK_RAN	8:c9db8784c33cea210872ac2d805439f8	addNotNullConstraint columnName=CLIENT_ID, tableName=OFFLINE_CLIENT_SESSION; addNotNullConstraint columnName=OWNER, tableName=RESOURCE_SERVER_PERM_TICKET; addNotNullConstraint columnName=REQUESTER, tableName=RESOURCE_SERVER_PERM_TICKET; addNotNull...		\N	4.8.0	\N	\N	0877552144
9.0.1-add-index-to-client.client_id	keycloak	META-INF/jpa-changelog-9.0.1.xml	2023-04-07 14:26:03.053423	81	EXECUTED	8:95b676ce8fc546a1fcfb4c92fae4add5	createIndex indexName=IDX_CLIENT_ID, tableName=CLIENT		\N	4.8.0	\N	\N	0877552144
9.0.1-KEYCLOAK-12579-drop-constraints	keycloak	META-INF/jpa-changelog-9.0.1.xml	2023-04-07 14:26:03.065804	82	MARK_RAN	8:38a6b2a41f5651018b1aca93a41401e5	dropUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP		\N	4.8.0	\N	\N	0877552144
9.0.1-KEYCLOAK-12579-add-not-null-constraint	keycloak	META-INF/jpa-changelog-9.0.1.xml	2023-04-07 14:26:03.096473	83	EXECUTED	8:3fb99bcad86a0229783123ac52f7609c	addNotNullConstraint columnName=PARENT_GROUP, tableName=KEYCLOAK_GROUP		\N	4.8.0	\N	\N	0877552144
9.0.1-KEYCLOAK-12579-recreate-constraints	keycloak	META-INF/jpa-changelog-9.0.1.xml	2023-04-07 14:26:03.098394	84	MARK_RAN	8:64f27a6fdcad57f6f9153210f2ec1bdb	addUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP		\N	4.8.0	\N	\N	0877552144
9.0.1-add-index-to-events	keycloak	META-INF/jpa-changelog-9.0.1.xml	2023-04-07 14:26:03.131009	85	EXECUTED	8:ab4f863f39adafd4c862f7ec01890abc	createIndex indexName=IDX_EVENT_TIME, tableName=EVENT_ENTITY		\N	4.8.0	\N	\N	0877552144
map-remove-ri	keycloak	META-INF/jpa-changelog-11.0.0.xml	2023-04-07 14:26:03.185498	86	EXECUTED	8:13c419a0eb336e91ee3a3bf8fda6e2a7	dropForeignKeyConstraint baseTableName=REALM, constraintName=FK_TRAF444KK6QRKMS7N56AIWQ5Y; dropForeignKeyConstraint baseTableName=KEYCLOAK_ROLE, constraintName=FK_KJHO5LE2C0RAL09FL8CM9WFW9		\N	4.8.0	\N	\N	0877552144
map-remove-ri	keycloak	META-INF/jpa-changelog-12.0.0.xml	2023-04-07 14:26:03.218312	87	EXECUTED	8:e3fb1e698e0471487f51af1ed80fe3ac	dropForeignKeyConstraint baseTableName=REALM_DEFAULT_GROUPS, constraintName=FK_DEF_GROUPS_GROUP; dropForeignKeyConstraint baseTableName=REALM_DEFAULT_ROLES, constraintName=FK_H4WPD7W4HSOOLNI3H0SW7BTJE; dropForeignKeyConstraint baseTableName=CLIENT...		\N	4.8.0	\N	\N	0877552144
12.1.0-add-realm-localization-table	keycloak	META-INF/jpa-changelog-12.0.0.xml	2023-04-07 14:26:03.246293	88	EXECUTED	8:babadb686aab7b56562817e60bf0abd0	createTable tableName=REALM_LOCALIZATIONS; addPrimaryKey tableName=REALM_LOCALIZATIONS		\N	4.8.0	\N	\N	0877552144
default-roles	keycloak	META-INF/jpa-changelog-13.0.0.xml	2023-04-07 14:26:03.276816	89	EXECUTED	8:72d03345fda8e2f17093d08801947773	addColumn tableName=REALM; customChange		\N	4.8.0	\N	\N	0877552144
default-roles-cleanup	keycloak	META-INF/jpa-changelog-13.0.0.xml	2023-04-07 14:26:03.311117	90	EXECUTED	8:61c9233951bd96ffecd9ba75f7d978a4	dropTable tableName=REALM_DEFAULT_ROLES; dropTable tableName=CLIENT_DEFAULT_ROLES		\N	4.8.0	\N	\N	0877552144
13.0.0-KEYCLOAK-16844	keycloak	META-INF/jpa-changelog-13.0.0.xml	2023-04-07 14:26:03.332992	91	EXECUTED	8:ea82e6ad945cec250af6372767b25525	createIndex indexName=IDX_OFFLINE_USS_PRELOAD, tableName=OFFLINE_USER_SESSION		\N	4.8.0	\N	\N	0877552144
map-remove-ri-13.0.0	keycloak	META-INF/jpa-changelog-13.0.0.xml	2023-04-07 14:26:03.384661	92	EXECUTED	8:d3f4a33f41d960ddacd7e2ef30d126b3	dropForeignKeyConstraint baseTableName=DEFAULT_CLIENT_SCOPE, constraintName=FK_R_DEF_CLI_SCOPE_SCOPE; dropForeignKeyConstraint baseTableName=CLIENT_SCOPE_CLIENT, constraintName=FK_C_CLI_SCOPE_SCOPE; dropForeignKeyConstraint baseTableName=CLIENT_SC...		\N	4.8.0	\N	\N	0877552144
13.0.0-KEYCLOAK-17992-drop-constraints	keycloak	META-INF/jpa-changelog-13.0.0.xml	2023-04-07 14:26:03.396429	93	MARK_RAN	8:1284a27fbd049d65831cb6fc07c8a783	dropPrimaryKey constraintName=C_CLI_SCOPE_BIND, tableName=CLIENT_SCOPE_CLIENT; dropIndex indexName=IDX_CLSCOPE_CL, tableName=CLIENT_SCOPE_CLIENT; dropIndex indexName=IDX_CL_CLSCOPE, tableName=CLIENT_SCOPE_CLIENT		\N	4.8.0	\N	\N	0877552144
13.0.0-increase-column-size-federated	keycloak	META-INF/jpa-changelog-13.0.0.xml	2023-04-07 14:26:03.425641	94	EXECUTED	8:9d11b619db2ae27c25853b8a37cd0dea	modifyDataType columnName=CLIENT_ID, tableName=CLIENT_SCOPE_CLIENT; modifyDataType columnName=SCOPE_ID, tableName=CLIENT_SCOPE_CLIENT		\N	4.8.0	\N	\N	0877552144
13.0.0-KEYCLOAK-17992-recreate-constraints	keycloak	META-INF/jpa-changelog-13.0.0.xml	2023-04-07 14:26:03.442445	95	MARK_RAN	8:3002bb3997451bb9e8bac5c5cd8d6327	addNotNullConstraint columnName=CLIENT_ID, tableName=CLIENT_SCOPE_CLIENT; addNotNullConstraint columnName=SCOPE_ID, tableName=CLIENT_SCOPE_CLIENT; addPrimaryKey constraintName=C_CLI_SCOPE_BIND, tableName=CLIENT_SCOPE_CLIENT; createIndex indexName=...		\N	4.8.0	\N	\N	0877552144
json-string-accomodation-fixed	keycloak	META-INF/jpa-changelog-13.0.0.xml	2023-04-07 14:26:03.478841	96	EXECUTED	8:dfbee0d6237a23ef4ccbb7a4e063c163	addColumn tableName=REALM_ATTRIBUTE; update tableName=REALM_ATTRIBUTE; dropColumn columnName=VALUE, tableName=REALM_ATTRIBUTE; renameColumn newColumnName=VALUE, oldColumnName=VALUE_NEW, tableName=REALM_ATTRIBUTE		\N	4.8.0	\N	\N	0877552144
14.0.0-KEYCLOAK-11019	keycloak	META-INF/jpa-changelog-14.0.0.xml	2023-04-07 14:26:03.533688	97	EXECUTED	8:75f3e372df18d38c62734eebb986b960	createIndex indexName=IDX_OFFLINE_CSS_PRELOAD, tableName=OFFLINE_CLIENT_SESSION; createIndex indexName=IDX_OFFLINE_USS_BY_USER, tableName=OFFLINE_USER_SESSION; createIndex indexName=IDX_OFFLINE_USS_BY_USERSESS, tableName=OFFLINE_USER_SESSION		\N	4.8.0	\N	\N	0877552144
14.0.0-KEYCLOAK-18286	keycloak	META-INF/jpa-changelog-14.0.0.xml	2023-04-07 14:26:03.564454	98	MARK_RAN	8:7fee73eddf84a6035691512c85637eef	createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES		\N	4.8.0	\N	\N	0877552144
14.0.0-KEYCLOAK-18286-revert	keycloak	META-INF/jpa-changelog-14.0.0.xml	2023-04-07 14:26:03.661685	99	MARK_RAN	8:7a11134ab12820f999fbf3bb13c3adc8	dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES		\N	4.8.0	\N	\N	0877552144
14.0.0-KEYCLOAK-18286-supported-dbs	keycloak	META-INF/jpa-changelog-14.0.0.xml	2023-04-07 14:26:03.699701	100	EXECUTED	8:c0f6eaac1f3be773ffe54cb5b8482b70	createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES		\N	4.8.0	\N	\N	0877552144
14.0.0-KEYCLOAK-18286-unsupported-dbs	keycloak	META-INF/jpa-changelog-14.0.0.xml	2023-04-07 14:26:03.721649	101	MARK_RAN	8:18186f0008b86e0f0f49b0c4d0e842ac	createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES		\N	4.8.0	\N	\N	0877552144
KEYCLOAK-17267-add-index-to-user-attributes	keycloak	META-INF/jpa-changelog-14.0.0.xml	2023-04-07 14:26:03.767892	102	EXECUTED	8:09c2780bcb23b310a7019d217dc7b433	createIndex indexName=IDX_USER_ATTRIBUTE_NAME, tableName=USER_ATTRIBUTE		\N	4.8.0	\N	\N	0877552144
KEYCLOAK-18146-add-saml-art-binding-identifier	keycloak	META-INF/jpa-changelog-14.0.0.xml	2023-04-07 14:26:03.801451	103	EXECUTED	8:276a44955eab693c970a42880197fff2	customChange		\N	4.8.0	\N	\N	0877552144
15.0.0-KEYCLOAK-18467	keycloak	META-INF/jpa-changelog-15.0.0.xml	2023-04-07 14:26:03.865951	104	EXECUTED	8:ba8ee3b694d043f2bfc1a1079d0760d7	addColumn tableName=REALM_LOCALIZATIONS; update tableName=REALM_LOCALIZATIONS; dropColumn columnName=TEXTS, tableName=REALM_LOCALIZATIONS; renameColumn newColumnName=TEXTS, oldColumnName=TEXTS_NEW, tableName=REALM_LOCALIZATIONS; addNotNullConstrai...		\N	4.8.0	\N	\N	0877552144
17.0.0-9562	keycloak	META-INF/jpa-changelog-17.0.0.xml	2023-04-07 14:26:03.908211	105	EXECUTED	8:5e06b1d75f5d17685485e610c2851b17	createIndex indexName=IDX_USER_SERVICE_ACCOUNT, tableName=USER_ENTITY		\N	4.8.0	\N	\N	0877552144
18.0.0-10625-IDX_ADMIN_EVENT_TIME	keycloak	META-INF/jpa-changelog-18.0.0.xml	2023-04-07 14:26:03.932272	106	EXECUTED	8:4b80546c1dc550ac552ee7b24a4ab7c0	createIndex indexName=IDX_ADMIN_EVENT_TIME, tableName=ADMIN_EVENT_ENTITY		\N	4.8.0	\N	\N	0877552144
19.0.0-10135	keycloak	META-INF/jpa-changelog-19.0.0.xml	2023-04-07 14:26:03.960756	107	EXECUTED	8:af510cd1bb2ab6339c45372f3e491696	customChange		\N	4.8.0	\N	\N	0877552144
20.0.0-12964-supported-dbs	keycloak	META-INF/jpa-changelog-20.0.0.xml	2023-04-07 14:26:04.017202	108	EXECUTED	8:05c99fc610845ef66ee812b7921af0ef	createIndex indexName=IDX_GROUP_ATT_BY_NAME_VALUE, tableName=GROUP_ATTRIBUTE		\N	4.8.0	\N	\N	0877552144
20.0.0-12964-unsupported-dbs	keycloak	META-INF/jpa-changelog-20.0.0.xml	2023-04-07 14:26:04.036673	109	MARK_RAN	8:314e803baf2f1ec315b3464e398b8247	createIndex indexName=IDX_GROUP_ATT_BY_NAME_VALUE, tableName=GROUP_ATTRIBUTE		\N	4.8.0	\N	\N	0877552144
client-attributes-string-accomodation-fixed	keycloak	META-INF/jpa-changelog-20.0.0.xml	2023-04-07 14:26:04.080501	110	EXECUTED	8:56e4677e7e12556f70b604c573840100	addColumn tableName=CLIENT_ATTRIBUTES; update tableName=CLIENT_ATTRIBUTES; dropColumn columnName=VALUE, tableName=CLIENT_ATTRIBUTES; renameColumn newColumnName=VALUE, oldColumnName=VALUE_NEW, tableName=CLIENT_ATTRIBUTES		\N	4.8.0	\N	\N	0877552144
21.0.2-17277	keycloak	META-INF/jpa-changelog-21.0.2.xml	2023-05-19 13:45:25.756864	111	EXECUTED	8:8806cb33d2a546ce770384bf98cf6eac	customChange		\N	4.16.1	\N	\N	4503924088
21.1.0-19404	keycloak	META-INF/jpa-changelog-21.1.0.xml	2023-05-19 13:45:25.806565	112	EXECUTED	8:fdb2924649d30555ab3a1744faba4928	modifyDataType columnName=DECISION_STRATEGY, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=LOGIC, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=POLICY_ENFORCE_MODE, tableName=RESOURCE_SERVER		\N	4.16.1	\N	\N	4503924088
21.1.0-19404-2	keycloak	META-INF/jpa-changelog-21.1.0.xml	2023-05-19 13:45:25.815415	113	MARK_RAN	8:1c96cc2b10903bd07a03670098d67fd6	addColumn tableName=RESOURCE_SERVER_POLICY; update tableName=RESOURCE_SERVER_POLICY; dropColumn columnName=DECISION_STRATEGY, tableName=RESOURCE_SERVER_POLICY; renameColumn newColumnName=DECISION_STRATEGY, oldColumnName=DECISION_STRATEGY_NEW, tabl...		\N	4.16.1	\N	\N	4503924088
\.


--
-- TOC entry 4149 (class 0 OID 16518)
-- Dependencies: 241
-- Data for Name: databasechangeloglock; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.databasechangeloglock (id, locked, lockgranted, lockedby) FROM stdin;
1	f	\N	\N
1000	f	\N	\N
1001	f	\N	\N
\.


--
-- TOC entry 4150 (class 0 OID 16521)
-- Dependencies: 242
-- Data for Name: default_client_scope; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.default_client_scope (realm_id, scope_id, default_scope) FROM stdin;
ae43bcfc-5430-4b91-987e-d6df1d2396aa	2f43062e-c262-4ef9-879e-3995f334be6c	f
ae43bcfc-5430-4b91-987e-d6df1d2396aa	cec79e00-452e-4bea-bced-ad2eba9f6cb4	t
ae43bcfc-5430-4b91-987e-d6df1d2396aa	af49489d-7ba4-47f1-a754-8411691885dd	t
ae43bcfc-5430-4b91-987e-d6df1d2396aa	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212	t
ae43bcfc-5430-4b91-987e-d6df1d2396aa	ceb503d8-d403-48c1-8c6c-dbb4f929b74b	f
ae43bcfc-5430-4b91-987e-d6df1d2396aa	062b2277-882e-4989-9743-97c4485847aa	f
ae43bcfc-5430-4b91-987e-d6df1d2396aa	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9	t
ae43bcfc-5430-4b91-987e-d6df1d2396aa	52fefbc2-756e-4fa1-935f-4596180dc8d0	t
ae43bcfc-5430-4b91-987e-d6df1d2396aa	4dae68af-19f6-4918-ae67-6c1fda0827d1	f
ae43bcfc-5430-4b91-987e-d6df1d2396aa	d41ba78d-a9e7-44cb-a060-e5538a51694c	t
3b82f5f8-9867-4aa1-a600-ae22c220133a	3887624b-89fe-4d58-83dc-27e34012554d	f
3b82f5f8-9867-4aa1-a600-ae22c220133a	a640577b-c323-4076-9861-10baac3e551b	t
3b82f5f8-9867-4aa1-a600-ae22c220133a	93a4cc1a-bddb-4c02-b782-25948d204836	t
3b82f5f8-9867-4aa1-a600-ae22c220133a	50d59a29-3563-47de-98c1-361823d6b5c3	t
3b82f5f8-9867-4aa1-a600-ae22c220133a	350748b2-db1c-4d5c-a1a0-d07f7a72878c	f
3b82f5f8-9867-4aa1-a600-ae22c220133a	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813	f
3b82f5f8-9867-4aa1-a600-ae22c220133a	ed9a51c3-2280-456c-b474-cc0069829d04	t
3b82f5f8-9867-4aa1-a600-ae22c220133a	68dd7df5-09d7-42d1-8b49-c03b890977c5	t
3b82f5f8-9867-4aa1-a600-ae22c220133a	53251e92-d6fd-4fc0-b8f2-f95428ca676f	f
3b82f5f8-9867-4aa1-a600-ae22c220133a	8ac4ad99-361d-4825-9733-22bc5888b62c	t
\.


--
-- TOC entry 4151 (class 0 OID 16525)
-- Dependencies: 243
-- Data for Name: event_entity; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.event_entity (id, client_id, details_json, error, ip_address, realm_id, session_id, event_time, type, user_id) FROM stdin;
\.


--
-- TOC entry 4152 (class 0 OID 16530)
-- Dependencies: 244
-- Data for Name: fed_user_attribute; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fed_user_attribute (id, name, user_id, realm_id, storage_provider_id, value) FROM stdin;
\.


--
-- TOC entry 4153 (class 0 OID 16535)
-- Dependencies: 245
-- Data for Name: fed_user_consent; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fed_user_consent (id, client_id, user_id, realm_id, storage_provider_id, created_date, last_updated_date, client_storage_provider, external_client_id) FROM stdin;
\.


--
-- TOC entry 4154 (class 0 OID 16540)
-- Dependencies: 246
-- Data for Name: fed_user_consent_cl_scope; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fed_user_consent_cl_scope (user_consent_id, scope_id) FROM stdin;
\.


--
-- TOC entry 4155 (class 0 OID 16543)
-- Dependencies: 247
-- Data for Name: fed_user_credential; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fed_user_credential (id, salt, type, created_date, user_id, realm_id, storage_provider_id, user_label, secret_data, credential_data, priority) FROM stdin;
\.


--
-- TOC entry 4156 (class 0 OID 16548)
-- Dependencies: 248
-- Data for Name: fed_user_group_membership; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fed_user_group_membership (group_id, user_id, realm_id, storage_provider_id) FROM stdin;
\.


--
-- TOC entry 4157 (class 0 OID 16551)
-- Dependencies: 249
-- Data for Name: fed_user_required_action; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fed_user_required_action (required_action, user_id, realm_id, storage_provider_id) FROM stdin;
\.


--
-- TOC entry 4158 (class 0 OID 16557)
-- Dependencies: 250
-- Data for Name: fed_user_role_mapping; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fed_user_role_mapping (role_id, user_id, realm_id, storage_provider_id) FROM stdin;
\.


--
-- TOC entry 4159 (class 0 OID 16560)
-- Dependencies: 251
-- Data for Name: federated_identity; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.federated_identity (identity_provider, realm_id, federated_user_id, federated_username, token, user_id) FROM stdin;
\.


--
-- TOC entry 4160 (class 0 OID 16565)
-- Dependencies: 252
-- Data for Name: federated_user; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.federated_user (id, storage_provider_id, realm_id) FROM stdin;
\.


--
-- TOC entry 4161 (class 0 OID 16570)
-- Dependencies: 253
-- Data for Name: group_attribute; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.group_attribute (id, name, value, group_id) FROM stdin;
\.


--
-- TOC entry 4162 (class 0 OID 16576)
-- Dependencies: 254
-- Data for Name: group_role_mapping; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.group_role_mapping (role_id, group_id) FROM stdin;
\.


--
-- TOC entry 4163 (class 0 OID 16579)
-- Dependencies: 255
-- Data for Name: identity_provider; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.identity_provider (internal_id, enabled, provider_alias, provider_id, store_token, authenticate_by_default, realm_id, add_token_role, trust_email, first_broker_login_flow_id, post_broker_login_flow_id, provider_display_name, link_only) FROM stdin;
\.


--
-- TOC entry 4164 (class 0 OID 16590)
-- Dependencies: 256
-- Data for Name: identity_provider_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.identity_provider_config (identity_provider_id, value, name) FROM stdin;
\.


--
-- TOC entry 4165 (class 0 OID 16595)
-- Dependencies: 257
-- Data for Name: identity_provider_mapper; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.identity_provider_mapper (id, name, idp_alias, idp_mapper_name, realm_id) FROM stdin;
\.


--
-- TOC entry 4166 (class 0 OID 16600)
-- Dependencies: 258
-- Data for Name: idp_mapper_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.idp_mapper_config (idp_mapper_id, value, name) FROM stdin;
\.


--
-- TOC entry 4167 (class 0 OID 16605)
-- Dependencies: 259
-- Data for Name: keycloak_group; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.keycloak_group (id, name, parent_group, realm_id) FROM stdin;
\.


--
-- TOC entry 4168 (class 0 OID 16608)
-- Dependencies: 260
-- Data for Name: keycloak_role; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) FROM stdin;
ad7e0bc1-f0c9-4092-875d-9fc377c138a4	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f	${role_default-roles}	default-roles-master	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N	\N
9766bcec-df11-44e6-bdb9-b8cef53b6938	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f	${role_create-realm}	create-realm	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N	\N
aef6b1da-0467-4657-892d-885cf1c071bf	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f	${role_admin}	admin	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N	\N
442f8179-a109-472e-9d77-5bbd26e84f6a	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_create-client}	create-client	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
720d7373-823f-4fe1-8d76-400296cf6278	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_view-realm}	view-realm	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
28d664ca-b3b8-41de-9590-0c68f5f4e3be	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_view-users}	view-users	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
b9f0d66e-0e40-4151-885a-c43bc073c328	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_view-clients}	view-clients	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
415cebac-5ce5-4cfc-9a24-abd1c8360711	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_view-events}	view-events	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
c414a13b-9b5c-4ca4-a475-5bb3557e7d93	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_view-identity-providers}	view-identity-providers	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
374ecce0-3cfd-42cd-a4ae-b99a44d3efa1	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_view-authorization}	view-authorization	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
3a997451-4a9c-4028-ae20-fffa5aa33572	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_manage-realm}	manage-realm	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
595a198a-cb14-4fb4-8db0-48a829ac8047	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_manage-users}	manage-users	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
59f6e85c-7721-46bc-b805-2a01b210c7cc	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_manage-clients}	manage-clients	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
ce649257-3734-41e1-8272-786afe8e80ff	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_manage-events}	manage-events	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
e9e95071-da01-4732-bbe7-8efc84402609	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_manage-identity-providers}	manage-identity-providers	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
2826eb6b-5259-4e86-87a9-66e66f2ea7bf	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_manage-authorization}	manage-authorization	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
743d1d59-404c-4328-a7d5-5df73a12364e	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_query-users}	query-users	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
d9587423-c239-437f-ab08-d461717a13cf	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_query-clients}	query-clients	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
2284f165-d964-4bb5-95f7-8deb0eec7212	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_query-realms}	query-realms	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
721c2dfe-1503-4bab-b0e8-2a7dc5215cbe	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_query-groups}	query-groups	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
cebf4ba6-7ac1-4630-ba87-e506897ad5bd	044054b7-770d-4204-a9fe-3257a210879e	t	${role_view-profile}	view-profile	ae43bcfc-5430-4b91-987e-d6df1d2396aa	044054b7-770d-4204-a9fe-3257a210879e	\N
ed2cdba8-f1d8-412a-bbcb-8711aaee53b9	044054b7-770d-4204-a9fe-3257a210879e	t	${role_manage-account}	manage-account	ae43bcfc-5430-4b91-987e-d6df1d2396aa	044054b7-770d-4204-a9fe-3257a210879e	\N
009c4c9a-5b7d-4eee-bf43-8681ee58170a	044054b7-770d-4204-a9fe-3257a210879e	t	${role_manage-account-links}	manage-account-links	ae43bcfc-5430-4b91-987e-d6df1d2396aa	044054b7-770d-4204-a9fe-3257a210879e	\N
27fa04dc-02a9-457c-a3b4-1576c2f2c4c6	044054b7-770d-4204-a9fe-3257a210879e	t	${role_view-applications}	view-applications	ae43bcfc-5430-4b91-987e-d6df1d2396aa	044054b7-770d-4204-a9fe-3257a210879e	\N
d37c80f7-670b-49d6-81a1-cd8237b24fc1	044054b7-770d-4204-a9fe-3257a210879e	t	${role_view-consent}	view-consent	ae43bcfc-5430-4b91-987e-d6df1d2396aa	044054b7-770d-4204-a9fe-3257a210879e	\N
3be8c17c-b04e-4d38-8a41-78553862fa77	044054b7-770d-4204-a9fe-3257a210879e	t	${role_manage-consent}	manage-consent	ae43bcfc-5430-4b91-987e-d6df1d2396aa	044054b7-770d-4204-a9fe-3257a210879e	\N
c3b21670-ba28-46e5-b724-d6e4123d3a12	044054b7-770d-4204-a9fe-3257a210879e	t	${role_view-groups}	view-groups	ae43bcfc-5430-4b91-987e-d6df1d2396aa	044054b7-770d-4204-a9fe-3257a210879e	\N
60059728-770b-4758-b95d-7cf7ddc697c3	044054b7-770d-4204-a9fe-3257a210879e	t	${role_delete-account}	delete-account	ae43bcfc-5430-4b91-987e-d6df1d2396aa	044054b7-770d-4204-a9fe-3257a210879e	\N
3817feea-6068-43f5-8a29-e5ee955ca82c	745a661c-5b71-46f0-b374-1055e8d22ee5	t	${role_read-token}	read-token	ae43bcfc-5430-4b91-987e-d6df1d2396aa	745a661c-5b71-46f0-b374-1055e8d22ee5	\N
ba613b55-6c4a-43c5-b5cc-1115cfe02303	1421d183-2492-4394-b963-a4a8cf677f34	t	${role_impersonation}	impersonation	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1421d183-2492-4394-b963-a4a8cf677f34	\N
34ad4237-b595-4fc0-9887-56997904fc7f	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f	${role_offline-access}	offline_access	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N	\N
0873fef2-e29c-430f-be93-4ddecae7769b	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f	${role_uma_authorization}	uma_authorization	ae43bcfc-5430-4b91-987e-d6df1d2396aa	\N	\N
2d27bb5b-0e7d-462c-9089-95a3c08a1559	3b82f5f8-9867-4aa1-a600-ae22c220133a	f	${role_default-roles}	default-roles-annettedemo	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N	\N
b1b14140-64f1-4fcc-a114-a83d52ca226a	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_create-client}	create-client	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
e8637836-7a03-4a64-b482-509cd6b1fb6a	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_view-realm}	view-realm	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
07e7d1b8-45a6-48d5-aa6e-797539995264	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_view-users}	view-users	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
8ea2c00b-913d-479c-abee-2362a5e09999	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_view-clients}	view-clients	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
0b77bc72-6d4c-48f5-8a78-d104dbdad976	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_view-events}	view-events	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
9bf7e0ad-f7c8-40e9-99d4-47b86a27c7a6	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_view-identity-providers}	view-identity-providers	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
07a8374a-b515-4aad-836d-87d06557e635	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_view-authorization}	view-authorization	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
d8d950ef-e465-45a2-973c-d8d31b72882d	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_manage-realm}	manage-realm	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
218957ec-bf6b-47ab-be91-89177b97ed9e	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_manage-users}	manage-users	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
ba4f5aef-a8a6-4ed3-b1c3-d48c31630c7f	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_manage-clients}	manage-clients	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
4cd4509c-3f99-4f24-9a58-f60907535092	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_manage-events}	manage-events	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
caa229c9-131d-4b81-b0ce-5345306ebaa2	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_manage-identity-providers}	manage-identity-providers	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
086713d1-88f1-4cf0-845b-bb8fb695d916	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_manage-authorization}	manage-authorization	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
a881b44f-d94d-4e71-9056-11b319d38e01	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_query-users}	query-users	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
132e1eb2-75de-4c1f-908b-17b82de33622	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_query-clients}	query-clients	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
ba225e8d-39a8-4347-9ace-a679e534173e	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_query-realms}	query-realms	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
5e46d28d-492a-433b-9b2b-5e731346739c	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_query-groups}	query-groups	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
033cca8b-3850-40da-86c1-fb79f9f97f3f	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_realm-admin}	realm-admin	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
cd5070a5-a3c6-4ad3-accd-5bb3e4d83ea0	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_create-client}	create-client	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
1fd28864-d863-4946-9fdd-92eac2c2efdf	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_view-realm}	view-realm	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
5b314a3c-f823-4a35-b98a-7c5846a843a4	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_view-users}	view-users	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
54c930c5-2699-4fb8-9fe9-8d60a6d9f6a6	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_view-clients}	view-clients	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
481ebbe7-bd4b-436c-abed-3908116847d4	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_view-events}	view-events	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
9d6e2e8e-b79e-4bca-9ac0-ea649bcea284	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_view-identity-providers}	view-identity-providers	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
55d2965c-776b-4c52-bd53-7b4ce7ff2c00	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_view-authorization}	view-authorization	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
df3247ee-3a1e-412e-9540-4b394c73cac8	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_manage-realm}	manage-realm	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
3a35e96f-1af7-4d22-9436-e5a28ae30e22	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_manage-users}	manage-users	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
08192651-3d7f-44f8-9d4e-116f13f2bca6	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_manage-clients}	manage-clients	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
106bbf17-ab80-4755-8612-3bcd27404907	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_manage-events}	manage-events	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
aad0631e-9448-4a67-8ab6-4fa820958c5f	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_manage-identity-providers}	manage-identity-providers	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
d192f453-df49-44e2-af9a-e1a65cf21779	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_manage-authorization}	manage-authorization	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
e9a8a30e-9768-49a4-a625-913d3af746ed	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_query-users}	query-users	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
a18010d6-459e-482e-810c-db68805b1ba8	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_query-clients}	query-clients	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
21747962-4038-4e7f-b1d3-4290d9063ed9	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_query-realms}	query-realms	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
7c97080e-6ab1-4dd4-9f4d-97276c98729f	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_query-groups}	query-groups	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
4e5f7252-77ed-4497-b866-cc39d8adaeb9	6cc96394-08b1-48bc-814e-9ed664c4d09c	t	${role_view-profile}	view-profile	3b82f5f8-9867-4aa1-a600-ae22c220133a	6cc96394-08b1-48bc-814e-9ed664c4d09c	\N
9f4ea579-5301-48e3-95c6-157e292faad2	6cc96394-08b1-48bc-814e-9ed664c4d09c	t	${role_manage-account}	manage-account	3b82f5f8-9867-4aa1-a600-ae22c220133a	6cc96394-08b1-48bc-814e-9ed664c4d09c	\N
7db418eb-7c63-425a-bcbd-0b287a6ac0c6	6cc96394-08b1-48bc-814e-9ed664c4d09c	t	${role_manage-account-links}	manage-account-links	3b82f5f8-9867-4aa1-a600-ae22c220133a	6cc96394-08b1-48bc-814e-9ed664c4d09c	\N
4043c296-b1ba-4e37-a98c-0c0adfe89e29	6cc96394-08b1-48bc-814e-9ed664c4d09c	t	${role_view-applications}	view-applications	3b82f5f8-9867-4aa1-a600-ae22c220133a	6cc96394-08b1-48bc-814e-9ed664c4d09c	\N
57771350-ea81-487c-b0b5-b845314d7ce2	6cc96394-08b1-48bc-814e-9ed664c4d09c	t	${role_view-consent}	view-consent	3b82f5f8-9867-4aa1-a600-ae22c220133a	6cc96394-08b1-48bc-814e-9ed664c4d09c	\N
6a6eac37-94ac-4d48-9d86-a2d84374309e	6cc96394-08b1-48bc-814e-9ed664c4d09c	t	${role_manage-consent}	manage-consent	3b82f5f8-9867-4aa1-a600-ae22c220133a	6cc96394-08b1-48bc-814e-9ed664c4d09c	\N
ffa5222d-2da5-40ae-975a-93b6166b5897	6cc96394-08b1-48bc-814e-9ed664c4d09c	t	${role_view-groups}	view-groups	3b82f5f8-9867-4aa1-a600-ae22c220133a	6cc96394-08b1-48bc-814e-9ed664c4d09c	\N
0688f9c8-3cf1-4da6-8c9e-72df854f8551	6cc96394-08b1-48bc-814e-9ed664c4d09c	t	${role_delete-account}	delete-account	3b82f5f8-9867-4aa1-a600-ae22c220133a	6cc96394-08b1-48bc-814e-9ed664c4d09c	\N
3a5eb7ea-7466-46c2-88a4-d7d9c44c57fd	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	t	${role_impersonation}	impersonation	ae43bcfc-5430-4b91-987e-d6df1d2396aa	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	\N
a77e4571-a302-4aa6-acf3-0c18fa4b147d	2ec72663-786b-47f8-9f53-39ce6ff11cbb	t	${role_impersonation}	impersonation	3b82f5f8-9867-4aa1-a600-ae22c220133a	2ec72663-786b-47f8-9f53-39ce6ff11cbb	\N
5e273bc0-268a-4d26-b280-6d93c314f70b	4788821b-b889-4cd0-9535-74949e39bc37	t	${role_read-token}	read-token	3b82f5f8-9867-4aa1-a600-ae22c220133a	4788821b-b889-4cd0-9535-74949e39bc37	\N
5763eed4-1479-433d-885c-fe30ee053a9a	3b82f5f8-9867-4aa1-a600-ae22c220133a	f	${role_offline-access}	offline_access	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N	\N
72fd8f57-f75a-4b21-a3bb-afbbce69fd3b	3b82f5f8-9867-4aa1-a600-ae22c220133a	f	${role_uma_authorization}	uma_authorization	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N	\N
2074a49d-0777-42e0-b5b9-944e4e267a2a	3b82f5f8-9867-4aa1-a600-ae22c220133a	f		admin	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N	\N
3a75d2fd-a8f3-4b27-baf6-415d5fca9f28	3b82f5f8-9867-4aa1-a600-ae22c220133a	f		user	3b82f5f8-9867-4aa1-a600-ae22c220133a	\N	\N
7498b540-9080-4fe6-947a-81e204706d72	629e8324-85ac-40be-8940-d6e8ab25eb96	t	\N	uma_protection	3b82f5f8-9867-4aa1-a600-ae22c220133a	629e8324-85ac-40be-8940-d6e8ab25eb96	\N
\.


--
-- TOC entry 4169 (class 0 OID 16614)
-- Dependencies: 261
-- Data for Name: migration_model; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.migration_model (id, version, update_time) FROM stdin;
jvjoo	20.0.2	1680877567
stcpr	21.1.1	1684503929
\.


--
-- TOC entry 4170 (class 0 OID 16618)
-- Dependencies: 262
-- Data for Name: offline_client_session; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.offline_client_session (user_session_id, client_id, offline_flag, "timestamp", data, client_storage_provider, external_client_id) FROM stdin;
\.


--
-- TOC entry 4171 (class 0 OID 16625)
-- Dependencies: 263
-- Data for Name: offline_user_session; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.offline_user_session (user_session_id, user_id, realm_id, created_on, offline_flag, data, last_session_refresh) FROM stdin;
\.


--
-- TOC entry 4172 (class 0 OID 16631)
-- Dependencies: 264
-- Data for Name: policy_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.policy_config (policy_id, name, value) FROM stdin;
\.


--
-- TOC entry 4173 (class 0 OID 16636)
-- Dependencies: 265
-- Data for Name: protocol_mapper; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) FROM stdin;
50ee406f-44db-4a7c-9f82-989a3358146a	audience resolve	openid-connect	oidc-audience-resolve-mapper	4cec789f-7e83-4565-9aa1-bda3b05b1adb	\N
ef906b90-f2bb-489f-a468-912d6adc7dea	locale	openid-connect	oidc-usermodel-attribute-mapper	d80bf699-b641-4ea2-9752-42cf143ab825	\N
9f3b75e8-884f-4af9-9323-0e0d2708e48c	role list	saml	saml-role-list-mapper	\N	cec79e00-452e-4bea-bced-ad2eba9f6cb4
df097ed7-5033-41e0-9201-f0c6cab58c97	full name	openid-connect	oidc-full-name-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
05a15b83-6818-4ae0-993d-ae018b2bb64d	family name	openid-connect	oidc-usermodel-property-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
996388e5-43b8-454e-9d38-2e96bb7ab57b	given name	openid-connect	oidc-usermodel-property-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
05de6ef3-9d88-410b-9c5e-fddd52c31f0f	middle name	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
3c05512c-0600-4579-a3e2-e42c86e8f2be	nickname	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
f3902e45-0305-40c2-9e3d-2274defb7062	username	openid-connect	oidc-usermodel-property-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
12b60979-9a0b-4ee6-ab30-82a0b4bc8474	profile	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
99361b56-a217-46c8-983d-5d1a05c7ba07	picture	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
afbbd052-e7ad-4ba2-9aae-fb99d926c863	website	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
bf3506e2-0c13-4186-9749-f170fcf261a8	gender	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
489ca5b7-a9c6-4b7f-8b91-57d2f75db438	birthdate	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
8ea28793-c081-447b-ba65-b6de3e2f2f79	zoneinfo	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
77b1a952-fc89-47ac-b489-82b2ae4fefc0	locale	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
e1941b99-8bde-456d-a233-20297008e4c4	updated at	openid-connect	oidc-usermodel-attribute-mapper	\N	af49489d-7ba4-47f1-a754-8411691885dd
b2a83eea-c1e8-401f-a895-7cd1398594af	email	openid-connect	oidc-usermodel-property-mapper	\N	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212
0618b6f1-3f6e-4fee-802d-41f35a3ab24e	email verified	openid-connect	oidc-usermodel-property-mapper	\N	2cd1ac26-a8c8-4038-8eaf-40a3ec7a7212
0c92c6bc-e298-44be-a2c3-191ad260eeca	address	openid-connect	oidc-address-mapper	\N	ceb503d8-d403-48c1-8c6c-dbb4f929b74b
9f127458-15d6-47de-91af-998cbe347ff9	phone number	openid-connect	oidc-usermodel-attribute-mapper	\N	062b2277-882e-4989-9743-97c4485847aa
881e7d38-382a-4f5e-bf6e-ff6802926024	phone number verified	openid-connect	oidc-usermodel-attribute-mapper	\N	062b2277-882e-4989-9743-97c4485847aa
5c13bc1d-5511-42d4-9975-67cbc9c04e4c	realm roles	openid-connect	oidc-usermodel-realm-role-mapper	\N	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9
74a43308-c309-4764-870f-4e96329597f5	client roles	openid-connect	oidc-usermodel-client-role-mapper	\N	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9
c3b4485f-2bca-47d4-bd03-9db37fb5c145	audience resolve	openid-connect	oidc-audience-resolve-mapper	\N	0f936f9e-6e4f-4b0c-b182-82db2e6f4ed9
74cb23ac-e4d5-46b6-9b7d-111d42ed9e5f	allowed web origins	openid-connect	oidc-allowed-origins-mapper	\N	52fefbc2-756e-4fa1-935f-4596180dc8d0
3f072654-88fa-427c-984d-d52d8399b751	upn	openid-connect	oidc-usermodel-property-mapper	\N	4dae68af-19f6-4918-ae67-6c1fda0827d1
1744e0b0-b83e-4842-b889-1102463dc824	groups	openid-connect	oidc-usermodel-realm-role-mapper	\N	4dae68af-19f6-4918-ae67-6c1fda0827d1
59e992d3-f3b6-42e7-b759-f4f771df1f44	acr loa level	openid-connect	oidc-acr-mapper	\N	d41ba78d-a9e7-44cb-a060-e5538a51694c
adb8c13f-cdb2-4888-b807-d2524f4fcc38	audience resolve	openid-connect	oidc-audience-resolve-mapper	da76b89f-97ee-473d-850d-8bb339a8f698	\N
5403920d-56c1-4d0b-ae8e-4952044e77cc	role list	saml	saml-role-list-mapper	\N	a640577b-c323-4076-9861-10baac3e551b
7ff06c59-6266-48eb-bc9a-9c4084240cdd	full name	openid-connect	oidc-full-name-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
61740ce9-96dd-43b2-bb6d-f638d866ac0a	family name	openid-connect	oidc-usermodel-property-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
74fac754-c4b3-4735-b388-4dc58e008cb7	given name	openid-connect	oidc-usermodel-property-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
eef5b293-8745-43a6-a44e-f428886d93db	middle name	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
691219b7-16d7-44bd-b192-53472a82a724	nickname	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
445ecdd0-6341-4943-a58a-ae7c4fa551c4	username	openid-connect	oidc-usermodel-property-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
b07f5ec1-e5b4-4600-9637-7b178dfc204e	profile	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
bbe2ad4f-e40a-4d6d-89f7-0476a8e87c71	picture	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
3350f326-9cf7-4aab-a69a-88b3e31129a1	website	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
b6603d43-c85f-48c0-8a7d-e6a61702d4b9	gender	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
2dabd335-8ad6-4a12-925e-2fbe55023541	birthdate	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
a6e266f5-8eef-4459-920b-166321740a97	zoneinfo	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
3e613b21-6910-4fbc-85c4-7249121ad1a3	locale	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
d4b14c06-6953-468b-b181-7238182f196b	updated at	openid-connect	oidc-usermodel-attribute-mapper	\N	93a4cc1a-bddb-4c02-b782-25948d204836
270e342f-156f-4feb-9da0-0fcf674b4509	email	openid-connect	oidc-usermodel-property-mapper	\N	50d59a29-3563-47de-98c1-361823d6b5c3
dddc43d6-f169-411e-9126-9ecb6b90cdbe	email verified	openid-connect	oidc-usermodel-property-mapper	\N	50d59a29-3563-47de-98c1-361823d6b5c3
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	address	openid-connect	oidc-address-mapper	\N	350748b2-db1c-4d5c-a1a0-d07f7a72878c
10c563d0-a94a-40b8-8837-f49ab6f0dbf0	phone number	openid-connect	oidc-usermodel-attribute-mapper	\N	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813
4f802985-ddc2-409f-aaa4-9c73f13e8e0c	phone number verified	openid-connect	oidc-usermodel-attribute-mapper	\N	d5a6a95c-2fe3-4ea0-a6fa-caed6061b813
6beae515-c8c1-41a3-8144-feeb5f90933b	realm roles	openid-connect	oidc-usermodel-realm-role-mapper	\N	ed9a51c3-2280-456c-b474-cc0069829d04
f0a16e8f-e50d-4152-a906-55759de25079	client roles	openid-connect	oidc-usermodel-client-role-mapper	\N	ed9a51c3-2280-456c-b474-cc0069829d04
8794fab0-64ef-4296-bb25-3e64d5b34479	audience resolve	openid-connect	oidc-audience-resolve-mapper	\N	ed9a51c3-2280-456c-b474-cc0069829d04
b4554e03-a191-4d45-92b8-c60f95a2f6e1	allowed web origins	openid-connect	oidc-allowed-origins-mapper	\N	68dd7df5-09d7-42d1-8b49-c03b890977c5
bd1dd2d6-eee1-482d-bb15-a6f79270f986	upn	openid-connect	oidc-usermodel-property-mapper	\N	53251e92-d6fd-4fc0-b8f2-f95428ca676f
dff1f352-ddb4-4f85-8126-94fc7376a540	groups	openid-connect	oidc-usermodel-realm-role-mapper	\N	53251e92-d6fd-4fc0-b8f2-f95428ca676f
3e21bc57-1a2e-43f9-8603-95c5cf39fa4c	acr loa level	openid-connect	oidc-acr-mapper	\N	8ac4ad99-361d-4825-9733-22bc5888b62c
6fbb27f0-d9b6-4648-a9c1-e6a01d21109f	locale	openid-connect	oidc-usermodel-attribute-mapper	b35f3c52-1869-42f0-8219-694107c37036	\N
19c6be3b-9dec-4a70-921a-bf128b6b33e5	Person ID	openid-connect	oidc-usermodel-attribute-mapper	\N	624026aa-d09e-41dc-a080-4dcd0d3af7f2
993b664a-cfb4-4305-9828-fdb07604f171	Client ID	openid-connect	oidc-usersessionmodel-note-mapper	629e8324-85ac-40be-8940-d6e8ab25eb96	\N
7f5759c6-4f4e-49e8-bae7-66e97c40ac7f	Client Host	openid-connect	oidc-usersessionmodel-note-mapper	629e8324-85ac-40be-8940-d6e8ab25eb96	\N
e7f70bcf-18e3-45f8-9053-a14abb097e5d	Client IP Address	openid-connect	oidc-usersessionmodel-note-mapper	629e8324-85ac-40be-8940-d6e8ab25eb96	\N
\.


--
-- TOC entry 4174 (class 0 OID 16641)
-- Dependencies: 266
-- Data for Name: protocol_mapper_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.protocol_mapper_config (protocol_mapper_id, value, name) FROM stdin;
ef906b90-f2bb-489f-a468-912d6adc7dea	true	userinfo.token.claim
ef906b90-f2bb-489f-a468-912d6adc7dea	locale	user.attribute
ef906b90-f2bb-489f-a468-912d6adc7dea	true	id.token.claim
ef906b90-f2bb-489f-a468-912d6adc7dea	true	access.token.claim
ef906b90-f2bb-489f-a468-912d6adc7dea	locale	claim.name
ef906b90-f2bb-489f-a468-912d6adc7dea	String	jsonType.label
9f3b75e8-884f-4af9-9323-0e0d2708e48c	false	single
9f3b75e8-884f-4af9-9323-0e0d2708e48c	Basic	attribute.nameformat
9f3b75e8-884f-4af9-9323-0e0d2708e48c	Role	attribute.name
05a15b83-6818-4ae0-993d-ae018b2bb64d	true	userinfo.token.claim
05a15b83-6818-4ae0-993d-ae018b2bb64d	lastName	user.attribute
05a15b83-6818-4ae0-993d-ae018b2bb64d	true	id.token.claim
05a15b83-6818-4ae0-993d-ae018b2bb64d	true	access.token.claim
05a15b83-6818-4ae0-993d-ae018b2bb64d	family_name	claim.name
05a15b83-6818-4ae0-993d-ae018b2bb64d	String	jsonType.label
05de6ef3-9d88-410b-9c5e-fddd52c31f0f	true	userinfo.token.claim
05de6ef3-9d88-410b-9c5e-fddd52c31f0f	middleName	user.attribute
05de6ef3-9d88-410b-9c5e-fddd52c31f0f	true	id.token.claim
05de6ef3-9d88-410b-9c5e-fddd52c31f0f	true	access.token.claim
05de6ef3-9d88-410b-9c5e-fddd52c31f0f	middle_name	claim.name
05de6ef3-9d88-410b-9c5e-fddd52c31f0f	String	jsonType.label
12b60979-9a0b-4ee6-ab30-82a0b4bc8474	true	userinfo.token.claim
12b60979-9a0b-4ee6-ab30-82a0b4bc8474	profile	user.attribute
12b60979-9a0b-4ee6-ab30-82a0b4bc8474	true	id.token.claim
12b60979-9a0b-4ee6-ab30-82a0b4bc8474	true	access.token.claim
12b60979-9a0b-4ee6-ab30-82a0b4bc8474	profile	claim.name
12b60979-9a0b-4ee6-ab30-82a0b4bc8474	String	jsonType.label
3c05512c-0600-4579-a3e2-e42c86e8f2be	true	userinfo.token.claim
3c05512c-0600-4579-a3e2-e42c86e8f2be	nickname	user.attribute
3c05512c-0600-4579-a3e2-e42c86e8f2be	true	id.token.claim
3c05512c-0600-4579-a3e2-e42c86e8f2be	true	access.token.claim
3c05512c-0600-4579-a3e2-e42c86e8f2be	nickname	claim.name
3c05512c-0600-4579-a3e2-e42c86e8f2be	String	jsonType.label
489ca5b7-a9c6-4b7f-8b91-57d2f75db438	true	userinfo.token.claim
489ca5b7-a9c6-4b7f-8b91-57d2f75db438	birthdate	user.attribute
489ca5b7-a9c6-4b7f-8b91-57d2f75db438	true	id.token.claim
489ca5b7-a9c6-4b7f-8b91-57d2f75db438	true	access.token.claim
489ca5b7-a9c6-4b7f-8b91-57d2f75db438	birthdate	claim.name
489ca5b7-a9c6-4b7f-8b91-57d2f75db438	String	jsonType.label
77b1a952-fc89-47ac-b489-82b2ae4fefc0	true	userinfo.token.claim
77b1a952-fc89-47ac-b489-82b2ae4fefc0	locale	user.attribute
77b1a952-fc89-47ac-b489-82b2ae4fefc0	true	id.token.claim
77b1a952-fc89-47ac-b489-82b2ae4fefc0	true	access.token.claim
77b1a952-fc89-47ac-b489-82b2ae4fefc0	locale	claim.name
77b1a952-fc89-47ac-b489-82b2ae4fefc0	String	jsonType.label
8ea28793-c081-447b-ba65-b6de3e2f2f79	true	userinfo.token.claim
8ea28793-c081-447b-ba65-b6de3e2f2f79	zoneinfo	user.attribute
8ea28793-c081-447b-ba65-b6de3e2f2f79	true	id.token.claim
8ea28793-c081-447b-ba65-b6de3e2f2f79	true	access.token.claim
8ea28793-c081-447b-ba65-b6de3e2f2f79	zoneinfo	claim.name
8ea28793-c081-447b-ba65-b6de3e2f2f79	String	jsonType.label
99361b56-a217-46c8-983d-5d1a05c7ba07	true	userinfo.token.claim
99361b56-a217-46c8-983d-5d1a05c7ba07	picture	user.attribute
99361b56-a217-46c8-983d-5d1a05c7ba07	true	id.token.claim
99361b56-a217-46c8-983d-5d1a05c7ba07	true	access.token.claim
99361b56-a217-46c8-983d-5d1a05c7ba07	picture	claim.name
99361b56-a217-46c8-983d-5d1a05c7ba07	String	jsonType.label
996388e5-43b8-454e-9d38-2e96bb7ab57b	true	userinfo.token.claim
996388e5-43b8-454e-9d38-2e96bb7ab57b	firstName	user.attribute
996388e5-43b8-454e-9d38-2e96bb7ab57b	true	id.token.claim
996388e5-43b8-454e-9d38-2e96bb7ab57b	true	access.token.claim
996388e5-43b8-454e-9d38-2e96bb7ab57b	given_name	claim.name
996388e5-43b8-454e-9d38-2e96bb7ab57b	String	jsonType.label
afbbd052-e7ad-4ba2-9aae-fb99d926c863	true	userinfo.token.claim
afbbd052-e7ad-4ba2-9aae-fb99d926c863	website	user.attribute
afbbd052-e7ad-4ba2-9aae-fb99d926c863	true	id.token.claim
afbbd052-e7ad-4ba2-9aae-fb99d926c863	true	access.token.claim
afbbd052-e7ad-4ba2-9aae-fb99d926c863	website	claim.name
afbbd052-e7ad-4ba2-9aae-fb99d926c863	String	jsonType.label
bf3506e2-0c13-4186-9749-f170fcf261a8	true	userinfo.token.claim
bf3506e2-0c13-4186-9749-f170fcf261a8	gender	user.attribute
bf3506e2-0c13-4186-9749-f170fcf261a8	true	id.token.claim
bf3506e2-0c13-4186-9749-f170fcf261a8	true	access.token.claim
bf3506e2-0c13-4186-9749-f170fcf261a8	gender	claim.name
bf3506e2-0c13-4186-9749-f170fcf261a8	String	jsonType.label
df097ed7-5033-41e0-9201-f0c6cab58c97	true	userinfo.token.claim
df097ed7-5033-41e0-9201-f0c6cab58c97	true	id.token.claim
df097ed7-5033-41e0-9201-f0c6cab58c97	true	access.token.claim
e1941b99-8bde-456d-a233-20297008e4c4	true	userinfo.token.claim
e1941b99-8bde-456d-a233-20297008e4c4	updatedAt	user.attribute
e1941b99-8bde-456d-a233-20297008e4c4	true	id.token.claim
e1941b99-8bde-456d-a233-20297008e4c4	true	access.token.claim
e1941b99-8bde-456d-a233-20297008e4c4	updated_at	claim.name
e1941b99-8bde-456d-a233-20297008e4c4	long	jsonType.label
f3902e45-0305-40c2-9e3d-2274defb7062	true	userinfo.token.claim
f3902e45-0305-40c2-9e3d-2274defb7062	username	user.attribute
f3902e45-0305-40c2-9e3d-2274defb7062	true	id.token.claim
f3902e45-0305-40c2-9e3d-2274defb7062	true	access.token.claim
f3902e45-0305-40c2-9e3d-2274defb7062	preferred_username	claim.name
f3902e45-0305-40c2-9e3d-2274defb7062	String	jsonType.label
0618b6f1-3f6e-4fee-802d-41f35a3ab24e	true	userinfo.token.claim
0618b6f1-3f6e-4fee-802d-41f35a3ab24e	emailVerified	user.attribute
0618b6f1-3f6e-4fee-802d-41f35a3ab24e	true	id.token.claim
0618b6f1-3f6e-4fee-802d-41f35a3ab24e	true	access.token.claim
0618b6f1-3f6e-4fee-802d-41f35a3ab24e	email_verified	claim.name
0618b6f1-3f6e-4fee-802d-41f35a3ab24e	boolean	jsonType.label
b2a83eea-c1e8-401f-a895-7cd1398594af	true	userinfo.token.claim
b2a83eea-c1e8-401f-a895-7cd1398594af	email	user.attribute
b2a83eea-c1e8-401f-a895-7cd1398594af	true	id.token.claim
b2a83eea-c1e8-401f-a895-7cd1398594af	true	access.token.claim
b2a83eea-c1e8-401f-a895-7cd1398594af	email	claim.name
b2a83eea-c1e8-401f-a895-7cd1398594af	String	jsonType.label
0c92c6bc-e298-44be-a2c3-191ad260eeca	formatted	user.attribute.formatted
0c92c6bc-e298-44be-a2c3-191ad260eeca	country	user.attribute.country
0c92c6bc-e298-44be-a2c3-191ad260eeca	postal_code	user.attribute.postal_code
0c92c6bc-e298-44be-a2c3-191ad260eeca	true	userinfo.token.claim
0c92c6bc-e298-44be-a2c3-191ad260eeca	street	user.attribute.street
0c92c6bc-e298-44be-a2c3-191ad260eeca	true	id.token.claim
0c92c6bc-e298-44be-a2c3-191ad260eeca	region	user.attribute.region
0c92c6bc-e298-44be-a2c3-191ad260eeca	true	access.token.claim
0c92c6bc-e298-44be-a2c3-191ad260eeca	locality	user.attribute.locality
881e7d38-382a-4f5e-bf6e-ff6802926024	true	userinfo.token.claim
881e7d38-382a-4f5e-bf6e-ff6802926024	phoneNumberVerified	user.attribute
881e7d38-382a-4f5e-bf6e-ff6802926024	true	id.token.claim
881e7d38-382a-4f5e-bf6e-ff6802926024	true	access.token.claim
881e7d38-382a-4f5e-bf6e-ff6802926024	phone_number_verified	claim.name
881e7d38-382a-4f5e-bf6e-ff6802926024	boolean	jsonType.label
9f127458-15d6-47de-91af-998cbe347ff9	true	userinfo.token.claim
9f127458-15d6-47de-91af-998cbe347ff9	phoneNumber	user.attribute
9f127458-15d6-47de-91af-998cbe347ff9	true	id.token.claim
9f127458-15d6-47de-91af-998cbe347ff9	true	access.token.claim
9f127458-15d6-47de-91af-998cbe347ff9	phone_number	claim.name
9f127458-15d6-47de-91af-998cbe347ff9	String	jsonType.label
5c13bc1d-5511-42d4-9975-67cbc9c04e4c	true	multivalued
5c13bc1d-5511-42d4-9975-67cbc9c04e4c	foo	user.attribute
5c13bc1d-5511-42d4-9975-67cbc9c04e4c	true	access.token.claim
5c13bc1d-5511-42d4-9975-67cbc9c04e4c	realm_access.roles	claim.name
5c13bc1d-5511-42d4-9975-67cbc9c04e4c	String	jsonType.label
74a43308-c309-4764-870f-4e96329597f5	true	multivalued
74a43308-c309-4764-870f-4e96329597f5	foo	user.attribute
74a43308-c309-4764-870f-4e96329597f5	true	access.token.claim
74a43308-c309-4764-870f-4e96329597f5	resource_access.${client_id}.roles	claim.name
74a43308-c309-4764-870f-4e96329597f5	String	jsonType.label
1744e0b0-b83e-4842-b889-1102463dc824	true	multivalued
1744e0b0-b83e-4842-b889-1102463dc824	foo	user.attribute
1744e0b0-b83e-4842-b889-1102463dc824	true	id.token.claim
1744e0b0-b83e-4842-b889-1102463dc824	true	access.token.claim
1744e0b0-b83e-4842-b889-1102463dc824	groups	claim.name
1744e0b0-b83e-4842-b889-1102463dc824	String	jsonType.label
3f072654-88fa-427c-984d-d52d8399b751	true	userinfo.token.claim
3f072654-88fa-427c-984d-d52d8399b751	username	user.attribute
3f072654-88fa-427c-984d-d52d8399b751	true	id.token.claim
3f072654-88fa-427c-984d-d52d8399b751	true	access.token.claim
3f072654-88fa-427c-984d-d52d8399b751	upn	claim.name
3f072654-88fa-427c-984d-d52d8399b751	String	jsonType.label
59e992d3-f3b6-42e7-b759-f4f771df1f44	true	id.token.claim
59e992d3-f3b6-42e7-b759-f4f771df1f44	true	access.token.claim
5403920d-56c1-4d0b-ae8e-4952044e77cc	false	single
5403920d-56c1-4d0b-ae8e-4952044e77cc	Basic	attribute.nameformat
5403920d-56c1-4d0b-ae8e-4952044e77cc	Role	attribute.name
2dabd335-8ad6-4a12-925e-2fbe55023541	true	userinfo.token.claim
2dabd335-8ad6-4a12-925e-2fbe55023541	birthdate	user.attribute
2dabd335-8ad6-4a12-925e-2fbe55023541	true	id.token.claim
2dabd335-8ad6-4a12-925e-2fbe55023541	true	access.token.claim
2dabd335-8ad6-4a12-925e-2fbe55023541	birthdate	claim.name
2dabd335-8ad6-4a12-925e-2fbe55023541	String	jsonType.label
3350f326-9cf7-4aab-a69a-88b3e31129a1	true	userinfo.token.claim
3350f326-9cf7-4aab-a69a-88b3e31129a1	website	user.attribute
3350f326-9cf7-4aab-a69a-88b3e31129a1	true	id.token.claim
3350f326-9cf7-4aab-a69a-88b3e31129a1	true	access.token.claim
3350f326-9cf7-4aab-a69a-88b3e31129a1	website	claim.name
3350f326-9cf7-4aab-a69a-88b3e31129a1	String	jsonType.label
3e613b21-6910-4fbc-85c4-7249121ad1a3	true	userinfo.token.claim
3e613b21-6910-4fbc-85c4-7249121ad1a3	locale	user.attribute
3e613b21-6910-4fbc-85c4-7249121ad1a3	true	id.token.claim
3e613b21-6910-4fbc-85c4-7249121ad1a3	true	access.token.claim
3e613b21-6910-4fbc-85c4-7249121ad1a3	locale	claim.name
3e613b21-6910-4fbc-85c4-7249121ad1a3	String	jsonType.label
445ecdd0-6341-4943-a58a-ae7c4fa551c4	true	userinfo.token.claim
445ecdd0-6341-4943-a58a-ae7c4fa551c4	username	user.attribute
445ecdd0-6341-4943-a58a-ae7c4fa551c4	true	id.token.claim
445ecdd0-6341-4943-a58a-ae7c4fa551c4	true	access.token.claim
445ecdd0-6341-4943-a58a-ae7c4fa551c4	preferred_username	claim.name
445ecdd0-6341-4943-a58a-ae7c4fa551c4	String	jsonType.label
61740ce9-96dd-43b2-bb6d-f638d866ac0a	true	userinfo.token.claim
61740ce9-96dd-43b2-bb6d-f638d866ac0a	lastName	user.attribute
61740ce9-96dd-43b2-bb6d-f638d866ac0a	true	id.token.claim
61740ce9-96dd-43b2-bb6d-f638d866ac0a	true	access.token.claim
61740ce9-96dd-43b2-bb6d-f638d866ac0a	family_name	claim.name
61740ce9-96dd-43b2-bb6d-f638d866ac0a	String	jsonType.label
691219b7-16d7-44bd-b192-53472a82a724	true	userinfo.token.claim
691219b7-16d7-44bd-b192-53472a82a724	nickname	user.attribute
691219b7-16d7-44bd-b192-53472a82a724	true	id.token.claim
691219b7-16d7-44bd-b192-53472a82a724	true	access.token.claim
691219b7-16d7-44bd-b192-53472a82a724	nickname	claim.name
691219b7-16d7-44bd-b192-53472a82a724	String	jsonType.label
74fac754-c4b3-4735-b388-4dc58e008cb7	true	userinfo.token.claim
74fac754-c4b3-4735-b388-4dc58e008cb7	firstName	user.attribute
74fac754-c4b3-4735-b388-4dc58e008cb7	true	id.token.claim
74fac754-c4b3-4735-b388-4dc58e008cb7	true	access.token.claim
74fac754-c4b3-4735-b388-4dc58e008cb7	given_name	claim.name
74fac754-c4b3-4735-b388-4dc58e008cb7	String	jsonType.label
7ff06c59-6266-48eb-bc9a-9c4084240cdd	true	userinfo.token.claim
7ff06c59-6266-48eb-bc9a-9c4084240cdd	true	id.token.claim
7ff06c59-6266-48eb-bc9a-9c4084240cdd	true	access.token.claim
a6e266f5-8eef-4459-920b-166321740a97	true	userinfo.token.claim
a6e266f5-8eef-4459-920b-166321740a97	zoneinfo	user.attribute
a6e266f5-8eef-4459-920b-166321740a97	true	id.token.claim
a6e266f5-8eef-4459-920b-166321740a97	true	access.token.claim
a6e266f5-8eef-4459-920b-166321740a97	zoneinfo	claim.name
a6e266f5-8eef-4459-920b-166321740a97	String	jsonType.label
b07f5ec1-e5b4-4600-9637-7b178dfc204e	true	userinfo.token.claim
b07f5ec1-e5b4-4600-9637-7b178dfc204e	profile	user.attribute
b07f5ec1-e5b4-4600-9637-7b178dfc204e	true	id.token.claim
b07f5ec1-e5b4-4600-9637-7b178dfc204e	true	access.token.claim
b07f5ec1-e5b4-4600-9637-7b178dfc204e	profile	claim.name
b07f5ec1-e5b4-4600-9637-7b178dfc204e	String	jsonType.label
b6603d43-c85f-48c0-8a7d-e6a61702d4b9	true	userinfo.token.claim
b6603d43-c85f-48c0-8a7d-e6a61702d4b9	gender	user.attribute
b6603d43-c85f-48c0-8a7d-e6a61702d4b9	true	id.token.claim
b6603d43-c85f-48c0-8a7d-e6a61702d4b9	true	access.token.claim
b6603d43-c85f-48c0-8a7d-e6a61702d4b9	gender	claim.name
b6603d43-c85f-48c0-8a7d-e6a61702d4b9	String	jsonType.label
bbe2ad4f-e40a-4d6d-89f7-0476a8e87c71	true	userinfo.token.claim
bbe2ad4f-e40a-4d6d-89f7-0476a8e87c71	picture	user.attribute
bbe2ad4f-e40a-4d6d-89f7-0476a8e87c71	true	id.token.claim
bbe2ad4f-e40a-4d6d-89f7-0476a8e87c71	true	access.token.claim
bbe2ad4f-e40a-4d6d-89f7-0476a8e87c71	picture	claim.name
bbe2ad4f-e40a-4d6d-89f7-0476a8e87c71	String	jsonType.label
d4b14c06-6953-468b-b181-7238182f196b	true	userinfo.token.claim
d4b14c06-6953-468b-b181-7238182f196b	updatedAt	user.attribute
d4b14c06-6953-468b-b181-7238182f196b	true	id.token.claim
d4b14c06-6953-468b-b181-7238182f196b	true	access.token.claim
d4b14c06-6953-468b-b181-7238182f196b	updated_at	claim.name
d4b14c06-6953-468b-b181-7238182f196b	long	jsonType.label
eef5b293-8745-43a6-a44e-f428886d93db	true	userinfo.token.claim
eef5b293-8745-43a6-a44e-f428886d93db	middleName	user.attribute
eef5b293-8745-43a6-a44e-f428886d93db	true	id.token.claim
eef5b293-8745-43a6-a44e-f428886d93db	true	access.token.claim
eef5b293-8745-43a6-a44e-f428886d93db	middle_name	claim.name
eef5b293-8745-43a6-a44e-f428886d93db	String	jsonType.label
270e342f-156f-4feb-9da0-0fcf674b4509	true	userinfo.token.claim
270e342f-156f-4feb-9da0-0fcf674b4509	email	user.attribute
270e342f-156f-4feb-9da0-0fcf674b4509	true	id.token.claim
270e342f-156f-4feb-9da0-0fcf674b4509	true	access.token.claim
270e342f-156f-4feb-9da0-0fcf674b4509	email	claim.name
270e342f-156f-4feb-9da0-0fcf674b4509	String	jsonType.label
dddc43d6-f169-411e-9126-9ecb6b90cdbe	true	userinfo.token.claim
dddc43d6-f169-411e-9126-9ecb6b90cdbe	emailVerified	user.attribute
dddc43d6-f169-411e-9126-9ecb6b90cdbe	true	id.token.claim
dddc43d6-f169-411e-9126-9ecb6b90cdbe	true	access.token.claim
dddc43d6-f169-411e-9126-9ecb6b90cdbe	email_verified	claim.name
dddc43d6-f169-411e-9126-9ecb6b90cdbe	boolean	jsonType.label
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	formatted	user.attribute.formatted
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	country	user.attribute.country
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	postal_code	user.attribute.postal_code
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	true	userinfo.token.claim
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	street	user.attribute.street
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	true	id.token.claim
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	region	user.attribute.region
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	true	access.token.claim
16cde18f-f4a2-4cf1-8b29-41b9d6d16d0e	locality	user.attribute.locality
10c563d0-a94a-40b8-8837-f49ab6f0dbf0	true	userinfo.token.claim
10c563d0-a94a-40b8-8837-f49ab6f0dbf0	phoneNumber	user.attribute
10c563d0-a94a-40b8-8837-f49ab6f0dbf0	true	id.token.claim
10c563d0-a94a-40b8-8837-f49ab6f0dbf0	true	access.token.claim
10c563d0-a94a-40b8-8837-f49ab6f0dbf0	phone_number	claim.name
10c563d0-a94a-40b8-8837-f49ab6f0dbf0	String	jsonType.label
4f802985-ddc2-409f-aaa4-9c73f13e8e0c	true	userinfo.token.claim
4f802985-ddc2-409f-aaa4-9c73f13e8e0c	phoneNumberVerified	user.attribute
4f802985-ddc2-409f-aaa4-9c73f13e8e0c	true	id.token.claim
4f802985-ddc2-409f-aaa4-9c73f13e8e0c	true	access.token.claim
4f802985-ddc2-409f-aaa4-9c73f13e8e0c	phone_number_verified	claim.name
4f802985-ddc2-409f-aaa4-9c73f13e8e0c	boolean	jsonType.label
6beae515-c8c1-41a3-8144-feeb5f90933b	true	multivalued
6beae515-c8c1-41a3-8144-feeb5f90933b	true	access.token.claim
6beae515-c8c1-41a3-8144-feeb5f90933b	realm_access.roles	claim.name
6beae515-c8c1-41a3-8144-feeb5f90933b	String	jsonType.label
f0a16e8f-e50d-4152-a906-55759de25079	true	multivalued
f0a16e8f-e50d-4152-a906-55759de25079	foo	user.attribute
f0a16e8f-e50d-4152-a906-55759de25079	true	access.token.claim
f0a16e8f-e50d-4152-a906-55759de25079	resource_access.${client_id}.roles	claim.name
f0a16e8f-e50d-4152-a906-55759de25079	String	jsonType.label
bd1dd2d6-eee1-482d-bb15-a6f79270f986	true	userinfo.token.claim
bd1dd2d6-eee1-482d-bb15-a6f79270f986	username	user.attribute
bd1dd2d6-eee1-482d-bb15-a6f79270f986	true	id.token.claim
bd1dd2d6-eee1-482d-bb15-a6f79270f986	true	access.token.claim
bd1dd2d6-eee1-482d-bb15-a6f79270f986	upn	claim.name
bd1dd2d6-eee1-482d-bb15-a6f79270f986	String	jsonType.label
dff1f352-ddb4-4f85-8126-94fc7376a540	true	multivalued
dff1f352-ddb4-4f85-8126-94fc7376a540	foo	user.attribute
dff1f352-ddb4-4f85-8126-94fc7376a540	true	id.token.claim
dff1f352-ddb4-4f85-8126-94fc7376a540	true	access.token.claim
dff1f352-ddb4-4f85-8126-94fc7376a540	groups	claim.name
dff1f352-ddb4-4f85-8126-94fc7376a540	String	jsonType.label
3e21bc57-1a2e-43f9-8603-95c5cf39fa4c	true	id.token.claim
3e21bc57-1a2e-43f9-8603-95c5cf39fa4c	true	access.token.claim
6fbb27f0-d9b6-4648-a9c1-e6a01d21109f	true	userinfo.token.claim
6fbb27f0-d9b6-4648-a9c1-e6a01d21109f	locale	user.attribute
6fbb27f0-d9b6-4648-a9c1-e6a01d21109f	true	id.token.claim
6fbb27f0-d9b6-4648-a9c1-e6a01d21109f	true	access.token.claim
6fbb27f0-d9b6-4648-a9c1-e6a01d21109f	locale	claim.name
6fbb27f0-d9b6-4648-a9c1-e6a01d21109f	String	jsonType.label
6beae515-c8c1-41a3-8144-feeb5f90933b	true	userinfo.token.claim
6beae515-c8c1-41a3-8144-feeb5f90933b	false	id.token.claim
19c6be3b-9dec-4a70-921a-bf128b6b33e5	false	aggregate.attrs
19c6be3b-9dec-4a70-921a-bf128b6b33e5	true	userinfo.token.claim
19c6be3b-9dec-4a70-921a-bf128b6b33e5	false	multivalued
19c6be3b-9dec-4a70-921a-bf128b6b33e5	person_id	user.attribute
19c6be3b-9dec-4a70-921a-bf128b6b33e5	true	id.token.claim
19c6be3b-9dec-4a70-921a-bf128b6b33e5	true	access.token.claim
19c6be3b-9dec-4a70-921a-bf128b6b33e5	person_id	claim.name
7f5759c6-4f4e-49e8-bae7-66e97c40ac7f	clientHost	user.session.note
7f5759c6-4f4e-49e8-bae7-66e97c40ac7f	true	id.token.claim
7f5759c6-4f4e-49e8-bae7-66e97c40ac7f	true	access.token.claim
7f5759c6-4f4e-49e8-bae7-66e97c40ac7f	clientHost	claim.name
7f5759c6-4f4e-49e8-bae7-66e97c40ac7f	String	jsonType.label
993b664a-cfb4-4305-9828-fdb07604f171	clientId	user.session.note
993b664a-cfb4-4305-9828-fdb07604f171	true	id.token.claim
993b664a-cfb4-4305-9828-fdb07604f171	true	access.token.claim
993b664a-cfb4-4305-9828-fdb07604f171	clientId	claim.name
993b664a-cfb4-4305-9828-fdb07604f171	String	jsonType.label
e7f70bcf-18e3-45f8-9053-a14abb097e5d	clientAddress	user.session.note
e7f70bcf-18e3-45f8-9053-a14abb097e5d	true	id.token.claim
e7f70bcf-18e3-45f8-9053-a14abb097e5d	true	access.token.claim
e7f70bcf-18e3-45f8-9053-a14abb097e5d	clientAddress	claim.name
e7f70bcf-18e3-45f8-9053-a14abb097e5d	String	jsonType.label
\.


--
-- TOC entry 4175 (class 0 OID 16646)
-- Dependencies: 267
-- Data for Name: realm; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm (id, access_code_lifespan, user_action_lifespan, access_token_lifespan, account_theme, admin_theme, email_theme, enabled, events_enabled, events_expiration, login_theme, name, not_before, password_policy, registration_allowed, remember_me, reset_password_allowed, social, ssl_required, sso_idle_timeout, sso_max_lifespan, update_profile_on_soc_login, verify_email, master_admin_client, login_lifespan, internationalization_enabled, default_locale, reg_email_as_username, admin_events_enabled, admin_events_details_enabled, edit_username_allowed, otp_policy_counter, otp_policy_window, otp_policy_period, otp_policy_digits, otp_policy_alg, otp_policy_type, browser_flow, registration_flow, direct_grant_flow, reset_credentials_flow, client_auth_flow, offline_session_idle_timeout, revoke_refresh_token, access_token_life_implicit, login_with_email_allowed, duplicate_emails_allowed, docker_auth_flow, refresh_token_max_reuse, allow_user_managed_access, sso_max_lifespan_remember_me, sso_idle_timeout_remember_me, default_role) FROM stdin;
3b82f5f8-9867-4aa1-a600-ae22c220133a	60	300	300				t	f	0	keycloak	AnnetteDemo	0	\N	f	f	f	f	EXTERNAL	1800	36000	f	f	7ffb87e9-4151-4eba-9f80-457fc7cb8e59	1800	f	\N	f	f	f	f	0	1	30	6	HmacSHA1	totp	3f99631b-cb27-4204-bc43-f8d64a40daa1	3ff5c90b-0efb-4f57-ac83-495260a0c33f	50299e02-9e95-4359-b25a-88974df0e081	45b6fb0c-a07f-455d-a073-9b17d17e82e3	d6c380a2-287e-44c4-b103-4ff7d6eaf28f	2592000	f	900	t	f	cd460e31-c98f-4060-bf9a-e1fa0c5a1a31	0	t	0	0	2d27bb5b-0e7d-462c-9089-95a3c08a1559
ae43bcfc-5430-4b91-987e-d6df1d2396aa	60	300	60	\N	\N	\N	t	f	0	\N	master	0	\N	f	f	f	f	EXTERNAL	1800	36000	f	f	1421d183-2492-4394-b963-a4a8cf677f34	1800	f	\N	f	f	f	f	0	1	30	6	HmacSHA1	totp	2e2d3905-b3c5-48e5-b482-033a961839e2	5a03c39d-e61c-459f-9848-db421cdc614e	9d2c5875-9a54-40b9-8557-d043e6fda5b5	922d78d6-3837-4ad6-b1c0-5519c3aa6f13	61023b39-a976-4a31-ad01-1b6d604de92f	2592000	f	900	t	f	ecc098a5-5c5f-48ac-8c1b-27f827ad16ee	0	f	0	0	ad7e0bc1-f0c9-4092-875d-9fc377c138a4
\.


--
-- TOC entry 4176 (class 0 OID 16679)
-- Dependencies: 268
-- Data for Name: realm_attribute; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm_attribute (name, realm_id, value) FROM stdin;
_browser_header.contentSecurityPolicyReportOnly	ae43bcfc-5430-4b91-987e-d6df1d2396aa	
_browser_header.xContentTypeOptions	ae43bcfc-5430-4b91-987e-d6df1d2396aa	nosniff
_browser_header.xRobotsTag	ae43bcfc-5430-4b91-987e-d6df1d2396aa	none
_browser_header.xFrameOptions	ae43bcfc-5430-4b91-987e-d6df1d2396aa	SAMEORIGIN
_browser_header.contentSecurityPolicy	ae43bcfc-5430-4b91-987e-d6df1d2396aa	frame-src 'self'; frame-ancestors 'self'; object-src 'none';
_browser_header.xXSSProtection	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1; mode=block
_browser_header.strictTransportSecurity	ae43bcfc-5430-4b91-987e-d6df1d2396aa	max-age=31536000; includeSubDomains
bruteForceProtected	ae43bcfc-5430-4b91-987e-d6df1d2396aa	false
permanentLockout	ae43bcfc-5430-4b91-987e-d6df1d2396aa	false
maxFailureWaitSeconds	ae43bcfc-5430-4b91-987e-d6df1d2396aa	900
minimumQuickLoginWaitSeconds	ae43bcfc-5430-4b91-987e-d6df1d2396aa	60
waitIncrementSeconds	ae43bcfc-5430-4b91-987e-d6df1d2396aa	60
quickLoginCheckMilliSeconds	ae43bcfc-5430-4b91-987e-d6df1d2396aa	1000
maxDeltaTimeSeconds	ae43bcfc-5430-4b91-987e-d6df1d2396aa	43200
failureFactor	ae43bcfc-5430-4b91-987e-d6df1d2396aa	30
realmReusableOtpCode	ae43bcfc-5430-4b91-987e-d6df1d2396aa	false
displayName	ae43bcfc-5430-4b91-987e-d6df1d2396aa	Keycloak
displayNameHtml	ae43bcfc-5430-4b91-987e-d6df1d2396aa	<div class="kc-logo-text"><span>Keycloak</span></div>
defaultSignatureAlgorithm	ae43bcfc-5430-4b91-987e-d6df1d2396aa	RS256
offlineSessionMaxLifespanEnabled	ae43bcfc-5430-4b91-987e-d6df1d2396aa	false
offlineSessionMaxLifespan	ae43bcfc-5430-4b91-987e-d6df1d2396aa	5184000
acr.loa.map	3b82f5f8-9867-4aa1-a600-ae22c220133a	{}
frontendUrl	3b82f5f8-9867-4aa1-a600-ae22c220133a	
realmReusableOtpCode	3b82f5f8-9867-4aa1-a600-ae22c220133a	false
oauth2DeviceCodeLifespan	3b82f5f8-9867-4aa1-a600-ae22c220133a	600
oauth2DevicePollingInterval	3b82f5f8-9867-4aa1-a600-ae22c220133a	5
cibaBackchannelTokenDeliveryMode	3b82f5f8-9867-4aa1-a600-ae22c220133a	poll
cibaExpiresIn	3b82f5f8-9867-4aa1-a600-ae22c220133a	120
cibaInterval	3b82f5f8-9867-4aa1-a600-ae22c220133a	5
cibaAuthRequestedUserHint	3b82f5f8-9867-4aa1-a600-ae22c220133a	login_hint
parRequestUriLifespan	3b82f5f8-9867-4aa1-a600-ae22c220133a	60
clientSessionIdleTimeout	3b82f5f8-9867-4aa1-a600-ae22c220133a	0
clientSessionMaxLifespan	3b82f5f8-9867-4aa1-a600-ae22c220133a	0
clientOfflineSessionIdleTimeout	3b82f5f8-9867-4aa1-a600-ae22c220133a	0
clientOfflineSessionMaxLifespan	3b82f5f8-9867-4aa1-a600-ae22c220133a	0
displayName	3b82f5f8-9867-4aa1-a600-ae22c220133a	
displayNameHtml	3b82f5f8-9867-4aa1-a600-ae22c220133a	
bruteForceProtected	3b82f5f8-9867-4aa1-a600-ae22c220133a	false
permanentLockout	3b82f5f8-9867-4aa1-a600-ae22c220133a	false
maxFailureWaitSeconds	3b82f5f8-9867-4aa1-a600-ae22c220133a	900
minimumQuickLoginWaitSeconds	3b82f5f8-9867-4aa1-a600-ae22c220133a	60
waitIncrementSeconds	3b82f5f8-9867-4aa1-a600-ae22c220133a	60
quickLoginCheckMilliSeconds	3b82f5f8-9867-4aa1-a600-ae22c220133a	1000
maxDeltaTimeSeconds	3b82f5f8-9867-4aa1-a600-ae22c220133a	43200
failureFactor	3b82f5f8-9867-4aa1-a600-ae22c220133a	30
actionTokenGeneratedByAdminLifespan	3b82f5f8-9867-4aa1-a600-ae22c220133a	43200
actionTokenGeneratedByUserLifespan	3b82f5f8-9867-4aa1-a600-ae22c220133a	300
defaultSignatureAlgorithm	3b82f5f8-9867-4aa1-a600-ae22c220133a	RS256
offlineSessionMaxLifespanEnabled	3b82f5f8-9867-4aa1-a600-ae22c220133a	false
offlineSessionMaxLifespan	3b82f5f8-9867-4aa1-a600-ae22c220133a	5184000
webAuthnPolicyRpEntityName	3b82f5f8-9867-4aa1-a600-ae22c220133a	keycloak
webAuthnPolicySignatureAlgorithms	3b82f5f8-9867-4aa1-a600-ae22c220133a	ES256
webAuthnPolicyRpId	3b82f5f8-9867-4aa1-a600-ae22c220133a	
webAuthnPolicyAttestationConveyancePreference	3b82f5f8-9867-4aa1-a600-ae22c220133a	not specified
webAuthnPolicyAuthenticatorAttachment	3b82f5f8-9867-4aa1-a600-ae22c220133a	not specified
webAuthnPolicyRequireResidentKey	3b82f5f8-9867-4aa1-a600-ae22c220133a	not specified
webAuthnPolicyUserVerificationRequirement	3b82f5f8-9867-4aa1-a600-ae22c220133a	not specified
webAuthnPolicyCreateTimeout	3b82f5f8-9867-4aa1-a600-ae22c220133a	0
webAuthnPolicyAvoidSameAuthenticatorRegister	3b82f5f8-9867-4aa1-a600-ae22c220133a	false
webAuthnPolicyRpEntityNamePasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	keycloak
webAuthnPolicySignatureAlgorithmsPasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	ES256
webAuthnPolicyRpIdPasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	
webAuthnPolicyAttestationConveyancePreferencePasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	not specified
webAuthnPolicyAuthenticatorAttachmentPasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	not specified
webAuthnPolicyRequireResidentKeyPasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	not specified
webAuthnPolicyUserVerificationRequirementPasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	not specified
webAuthnPolicyCreateTimeoutPasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	0
webAuthnPolicyAvoidSameAuthenticatorRegisterPasswordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	false
client-policies.profiles	3b82f5f8-9867-4aa1-a600-ae22c220133a	{"profiles":[]}
client-policies.policies	3b82f5f8-9867-4aa1-a600-ae22c220133a	{"policies":[]}
_browser_header.contentSecurityPolicyReportOnly	3b82f5f8-9867-4aa1-a600-ae22c220133a	
_browser_header.xContentTypeOptions	3b82f5f8-9867-4aa1-a600-ae22c220133a	nosniff
_browser_header.xRobotsTag	3b82f5f8-9867-4aa1-a600-ae22c220133a	none
_browser_header.xFrameOptions	3b82f5f8-9867-4aa1-a600-ae22c220133a	SAMEORIGIN
_browser_header.contentSecurityPolicy	3b82f5f8-9867-4aa1-a600-ae22c220133a	frame-src 'self'; frame-ancestors 'self'; object-src 'none';
_browser_header.xXSSProtection	3b82f5f8-9867-4aa1-a600-ae22c220133a	1; mode=block
_browser_header.strictTransportSecurity	3b82f5f8-9867-4aa1-a600-ae22c220133a	max-age=31536000; includeSubDomains
\.


--
-- TOC entry 4177 (class 0 OID 16684)
-- Dependencies: 269
-- Data for Name: realm_default_groups; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm_default_groups (realm_id, group_id) FROM stdin;
\.


--
-- TOC entry 4178 (class 0 OID 16687)
-- Dependencies: 270
-- Data for Name: realm_enabled_event_types; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm_enabled_event_types (realm_id, value) FROM stdin;
\.


--
-- TOC entry 4179 (class 0 OID 16690)
-- Dependencies: 271
-- Data for Name: realm_events_listeners; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm_events_listeners (realm_id, value) FROM stdin;
ae43bcfc-5430-4b91-987e-d6df1d2396aa	jboss-logging
3b82f5f8-9867-4aa1-a600-ae22c220133a	jboss-logging
\.


--
-- TOC entry 4180 (class 0 OID 16693)
-- Dependencies: 272
-- Data for Name: realm_localizations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm_localizations (realm_id, locale, texts) FROM stdin;
\.


--
-- TOC entry 4181 (class 0 OID 16698)
-- Dependencies: 273
-- Data for Name: realm_required_credential; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm_required_credential (type, form_label, input, secret, realm_id) FROM stdin;
password	password	t	t	ae43bcfc-5430-4b91-987e-d6df1d2396aa
password	password	t	t	3b82f5f8-9867-4aa1-a600-ae22c220133a
\.


--
-- TOC entry 4182 (class 0 OID 16705)
-- Dependencies: 274
-- Data for Name: realm_smtp_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm_smtp_config (realm_id, value, name) FROM stdin;
\.


--
-- TOC entry 4183 (class 0 OID 16710)
-- Dependencies: 275
-- Data for Name: realm_supported_locales; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.realm_supported_locales (realm_id, value) FROM stdin;
\.


--
-- TOC entry 4184 (class 0 OID 16713)
-- Dependencies: 276
-- Data for Name: redirect_uris; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.redirect_uris (client_id, value) FROM stdin;
044054b7-770d-4204-a9fe-3257a210879e	/realms/master/account/*
4cec789f-7e83-4565-9aa1-bda3b05b1adb	/realms/master/account/*
d80bf699-b641-4ea2-9752-42cf143ab825	/admin/master/console/*
6cc96394-08b1-48bc-814e-9ed664c4d09c	/realms/AnnetteDemo/account/*
da76b89f-97ee-473d-850d-8bb339a8f698	/realms/AnnetteDemo/account/*
b35f3c52-1869-42f0-8219-694107c37036	/admin/AnnetteDemo/console/*
629e8324-85ac-40be-8940-d6e8ab25eb96	http://localhost:3000/*
629e8324-85ac-40be-8940-d6e8ab25eb96	http://localhost:8500/*
\.


--
-- TOC entry 4185 (class 0 OID 16716)
-- Dependencies: 277
-- Data for Name: required_action_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.required_action_config (required_action_id, value, name) FROM stdin;
\.


--
-- TOC entry 4186 (class 0 OID 16721)
-- Dependencies: 278
-- Data for Name: required_action_provider; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) FROM stdin;
d52dbabe-d0c4-47c1-8fbb-81830e994bf8	VERIFY_EMAIL	Verify Email	ae43bcfc-5430-4b91-987e-d6df1d2396aa	t	f	VERIFY_EMAIL	50
4a68de8b-bcc6-46e3-aaa0-43546f2afdf8	UPDATE_PROFILE	Update Profile	ae43bcfc-5430-4b91-987e-d6df1d2396aa	t	f	UPDATE_PROFILE	40
ffea6819-9259-486c-82c9-0a2aa1fa01cd	CONFIGURE_TOTP	Configure OTP	ae43bcfc-5430-4b91-987e-d6df1d2396aa	t	f	CONFIGURE_TOTP	10
3e5d1c47-cfa9-451e-b577-9e67c357e829	UPDATE_PASSWORD	Update Password	ae43bcfc-5430-4b91-987e-d6df1d2396aa	t	f	UPDATE_PASSWORD	30
e1829720-cbd2-4d4e-a0de-d18c34ed255c	delete_account	Delete Account	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f	f	delete_account	60
545f0262-6082-4f21-9507-48fa3044082b	update_user_locale	Update User Locale	ae43bcfc-5430-4b91-987e-d6df1d2396aa	t	f	update_user_locale	1000
6a457584-c339-430b-8bfd-7fbbb846d9bc	webauthn-register	Webauthn Register	ae43bcfc-5430-4b91-987e-d6df1d2396aa	t	f	webauthn-register	70
3ab3015c-065f-47fe-90b4-d0bc8b08ee85	webauthn-register-passwordless	Webauthn Register Passwordless	ae43bcfc-5430-4b91-987e-d6df1d2396aa	t	f	webauthn-register-passwordless	80
d703c435-9f51-41a8-932f-b62a06626d59	VERIFY_EMAIL	Verify Email	3b82f5f8-9867-4aa1-a600-ae22c220133a	t	f	VERIFY_EMAIL	50
9f8e5458-fef3-484a-b0d8-c221f37de196	UPDATE_PROFILE	Update Profile	3b82f5f8-9867-4aa1-a600-ae22c220133a	t	f	UPDATE_PROFILE	40
1f39c5b1-369b-47f6-acfc-157ff5d7236d	CONFIGURE_TOTP	Configure OTP	3b82f5f8-9867-4aa1-a600-ae22c220133a	t	f	CONFIGURE_TOTP	10
94c71533-f22a-4c49-9162-ee52dc9acf5a	UPDATE_PASSWORD	Update Password	3b82f5f8-9867-4aa1-a600-ae22c220133a	t	f	UPDATE_PASSWORD	30
d8494adb-6821-4201-8432-30121dcda9ca	delete_account	Delete Account	3b82f5f8-9867-4aa1-a600-ae22c220133a	f	f	delete_account	60
86e129b1-26e5-4a1d-84ac-0e86ec5a04b8	update_user_locale	Update User Locale	3b82f5f8-9867-4aa1-a600-ae22c220133a	t	f	update_user_locale	1000
2dbc96a9-535d-4497-862d-4ed19911f2ce	webauthn-register	Webauthn Register	3b82f5f8-9867-4aa1-a600-ae22c220133a	t	f	webauthn-register	70
c2679558-31d9-49e2-8188-13218046f54d	webauthn-register-passwordless	Webauthn Register Passwordless	3b82f5f8-9867-4aa1-a600-ae22c220133a	t	f	webauthn-register-passwordless	80
30152a90-6b03-4d05-beb1-fb730cc62638	TERMS_AND_CONDITIONS	Terms and Conditions	ae43bcfc-5430-4b91-987e-d6df1d2396aa	f	f	TERMS_AND_CONDITIONS	20
587d6027-d645-4f4d-b593-870289f53cc0	TERMS_AND_CONDITIONS	Terms and Conditions	3b82f5f8-9867-4aa1-a600-ae22c220133a	f	f	TERMS_AND_CONDITIONS	20
\.


--
-- TOC entry 4187 (class 0 OID 16728)
-- Dependencies: 279
-- Data for Name: resource_attribute; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_attribute (id, name, value, resource_id) FROM stdin;
\.


--
-- TOC entry 4188 (class 0 OID 16734)
-- Dependencies: 280
-- Data for Name: resource_policy; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_policy (resource_id, policy_id) FROM stdin;
\.


--
-- TOC entry 4189 (class 0 OID 16737)
-- Dependencies: 281
-- Data for Name: resource_scope; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_scope (resource_id, scope_id) FROM stdin;
\.


--
-- TOC entry 4190 (class 0 OID 16740)
-- Dependencies: 282
-- Data for Name: resource_server; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_server (id, allow_rs_remote_mgmt, policy_enforce_mode, decision_strategy) FROM stdin;
\.


--
-- TOC entry 4191 (class 0 OID 16745)
-- Dependencies: 283
-- Data for Name: resource_server_perm_ticket; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_server_perm_ticket (id, owner, requester, created_timestamp, granted_timestamp, resource_id, scope_id, resource_server_id, policy_id) FROM stdin;
\.


--
-- TOC entry 4192 (class 0 OID 16750)
-- Dependencies: 284
-- Data for Name: resource_server_policy; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_server_policy (id, name, description, type, decision_strategy, logic, resource_server_id, owner) FROM stdin;
\.


--
-- TOC entry 4193 (class 0 OID 16755)
-- Dependencies: 285
-- Data for Name: resource_server_resource; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_server_resource (id, name, type, icon_uri, owner, resource_server_id, owner_managed_access, display_name) FROM stdin;
\.


--
-- TOC entry 4194 (class 0 OID 16761)
-- Dependencies: 286
-- Data for Name: resource_server_scope; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_server_scope (id, name, icon_uri, resource_server_id, display_name) FROM stdin;
\.


--
-- TOC entry 4195 (class 0 OID 16766)
-- Dependencies: 287
-- Data for Name: resource_uris; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resource_uris (resource_id, value) FROM stdin;
\.


--
-- TOC entry 4196 (class 0 OID 16769)
-- Dependencies: 288
-- Data for Name: role_attribute; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.role_attribute (id, role_id, name, value) FROM stdin;
\.


--
-- TOC entry 4197 (class 0 OID 16774)
-- Dependencies: 289
-- Data for Name: scope_mapping; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.scope_mapping (client_id, role_id) FROM stdin;
4cec789f-7e83-4565-9aa1-bda3b05b1adb	ed2cdba8-f1d8-412a-bbcb-8711aaee53b9
4cec789f-7e83-4565-9aa1-bda3b05b1adb	c3b21670-ba28-46e5-b724-d6e4123d3a12
da76b89f-97ee-473d-850d-8bb339a8f698	9f4ea579-5301-48e3-95c6-157e292faad2
da76b89f-97ee-473d-850d-8bb339a8f698	ffa5222d-2da5-40ae-975a-93b6166b5897
\.


--
-- TOC entry 4198 (class 0 OID 16777)
-- Dependencies: 290
-- Data for Name: scope_policy; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.scope_policy (scope_id, policy_id) FROM stdin;
\.


--
-- TOC entry 4199 (class 0 OID 16780)
-- Dependencies: 291
-- Data for Name: user_attribute; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_attribute (name, value, user_id, id) FROM stdin;
person_id	P0001	ad671e48-45f8-40f9-a807-414c48862820	dc44b317-815c-46ad-bdef-26985fa4ec17
person_id	P0002	873d1a17-05e2-4aec-b2d9-89aae8684c43	0ebdf399-5189-405c-a651-d54344ba74df
\.


--
-- TOC entry 4200 (class 0 OID 16786)
-- Dependencies: 292
-- Data for Name: user_consent; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_consent (id, client_id, user_id, created_date, last_updated_date, client_storage_provider, external_client_id) FROM stdin;
\.


--
-- TOC entry 4201 (class 0 OID 16791)
-- Dependencies: 293
-- Data for Name: user_consent_client_scope; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_consent_client_scope (user_consent_id, scope_id) FROM stdin;
\.


--
-- TOC entry 4202 (class 0 OID 16794)
-- Dependencies: 294
-- Data for Name: user_entity; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_entity (id, email, email_constraint, email_verified, enabled, federation_link, first_name, last_name, realm_id, username, created_timestamp, service_account_client_link, not_before) FROM stdin;
d2de545f-d20a-4a4e-9f74-188ef8b1aef1	\N	079897ef-5293-4674-ba9f-6486cabb8e0e	f	t	\N	\N	\N	ae43bcfc-5430-4b91-987e-d6df1d2396aa	admin	1680877588138	\N	0
ad671e48-45f8-40f9-a807-414c48862820	kristina.fisher@example.com	kristina.fisher@example.com	t	t	\N	Kristina	Fisher	3b82f5f8-9867-4aa1-a600-ae22c220133a	kristina.fisher	1682181018717	\N	0
873d1a17-05e2-4aec-b2d9-89aae8684c43	leah.martin@example.com	leah.martin@example.com	t	t	\N	Leah	Martin	3b82f5f8-9867-4aa1-a600-ae22c220133a	leah.martin	1684491279096	\N	0
\.


--
-- TOC entry 4203 (class 0 OID 16802)
-- Dependencies: 295
-- Data for Name: user_federation_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_federation_config (user_federation_provider_id, value, name) FROM stdin;
\.


--
-- TOC entry 4204 (class 0 OID 16807)
-- Dependencies: 296
-- Data for Name: user_federation_mapper; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_federation_mapper (id, name, federation_provider_id, federation_mapper_type, realm_id) FROM stdin;
\.


--
-- TOC entry 4205 (class 0 OID 16812)
-- Dependencies: 297
-- Data for Name: user_federation_mapper_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_federation_mapper_config (user_federation_mapper_id, value, name) FROM stdin;
\.


--
-- TOC entry 4206 (class 0 OID 16817)
-- Dependencies: 298
-- Data for Name: user_federation_provider; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_federation_provider (id, changed_sync_period, display_name, full_sync_period, last_sync, priority, provider_name, realm_id) FROM stdin;
\.


--
-- TOC entry 4207 (class 0 OID 16822)
-- Dependencies: 299
-- Data for Name: user_group_membership; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_group_membership (group_id, user_id) FROM stdin;
\.


--
-- TOC entry 4208 (class 0 OID 16825)
-- Dependencies: 300
-- Data for Name: user_required_action; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_required_action (user_id, required_action) FROM stdin;
\.


--
-- TOC entry 4209 (class 0 OID 16829)
-- Dependencies: 301
-- Data for Name: user_role_mapping; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_role_mapping (role_id, user_id) FROM stdin;
ad7e0bc1-f0c9-4092-875d-9fc377c138a4	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
aef6b1da-0467-4657-892d-885cf1c071bf	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
b1b14140-64f1-4fcc-a114-a83d52ca226a	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
e8637836-7a03-4a64-b482-509cd6b1fb6a	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
07e7d1b8-45a6-48d5-aa6e-797539995264	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
8ea2c00b-913d-479c-abee-2362a5e09999	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
0b77bc72-6d4c-48f5-8a78-d104dbdad976	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
9bf7e0ad-f7c8-40e9-99d4-47b86a27c7a6	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
07a8374a-b515-4aad-836d-87d06557e635	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
d8d950ef-e465-45a2-973c-d8d31b72882d	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
218957ec-bf6b-47ab-be91-89177b97ed9e	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
ba4f5aef-a8a6-4ed3-b1c3-d48c31630c7f	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
4cd4509c-3f99-4f24-9a58-f60907535092	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
caa229c9-131d-4b81-b0ce-5345306ebaa2	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
086713d1-88f1-4cf0-845b-bb8fb695d916	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
a881b44f-d94d-4e71-9056-11b319d38e01	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
132e1eb2-75de-4c1f-908b-17b82de33622	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
ba225e8d-39a8-4347-9ace-a679e534173e	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
5e46d28d-492a-433b-9b2b-5e731346739c	d2de545f-d20a-4a4e-9f74-188ef8b1aef1
2d27bb5b-0e7d-462c-9089-95a3c08a1559	ad671e48-45f8-40f9-a807-414c48862820
2074a49d-0777-42e0-b5b9-944e4e267a2a	ad671e48-45f8-40f9-a807-414c48862820
3a75d2fd-a8f3-4b27-baf6-415d5fca9f28	ad671e48-45f8-40f9-a807-414c48862820
2d27bb5b-0e7d-462c-9089-95a3c08a1559	873d1a17-05e2-4aec-b2d9-89aae8684c43
\.


--
-- TOC entry 4210 (class 0 OID 16832)
-- Dependencies: 302
-- Data for Name: user_session; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_session (id, auth_method, ip_address, last_session_refresh, login_username, realm_id, remember_me, started, user_id, user_session_state, broker_session_id, broker_user_id) FROM stdin;
\.


--
-- TOC entry 4211 (class 0 OID 16838)
-- Dependencies: 303
-- Data for Name: user_session_note; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_session_note (user_session, name, value) FROM stdin;
\.


--
-- TOC entry 4212 (class 0 OID 16843)
-- Dependencies: 304
-- Data for Name: username_login_failure; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.username_login_failure (realm_id, username, failed_login_not_before, last_failure, last_ip_failure, num_failures) FROM stdin;
\.


--
-- TOC entry 4213 (class 0 OID 16848)
-- Dependencies: 305
-- Data for Name: web_origins; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.web_origins (client_id, value) FROM stdin;
d80bf699-b641-4ea2-9752-42cf143ab825	+
b35f3c52-1869-42f0-8219-694107c37036	+
629e8324-85ac-40be-8940-d6e8ab25eb96	http://localhost:8500
629e8324-85ac-40be-8940-d6e8ab25eb96	http://localhost:3000
\.


--
-- TOC entry 3902 (class 2606 OID 16852)
-- Name: username_login_failure CONSTRAINT_17-2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.username_login_failure
    ADD CONSTRAINT "CONSTRAINT_17-2" PRIMARY KEY (realm_id, username);


--
-- TOC entry 3755 (class 2606 OID 16854)
-- Name: keycloak_role UK_J3RWUVD56ONTGSUHOGM184WW2-2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT "UK_J3RWUVD56ONTGSUHOGM184WW2-2" UNIQUE (name, client_realm_constraint);


--
-- TOC entry 3641 (class 2606 OID 16856)
-- Name: client_auth_flow_bindings c_cli_flow_bind; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_auth_flow_bindings
    ADD CONSTRAINT c_cli_flow_bind PRIMARY KEY (client_id, binding_name);


--
-- TOC entry 3656 (class 2606 OID 16858)
-- Name: client_scope_client c_cli_scope_bind; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_scope_client
    ADD CONSTRAINT c_cli_scope_bind PRIMARY KEY (client_id, scope_id);


--
-- TOC entry 3643 (class 2606 OID 16860)
-- Name: client_initial_access cnstr_client_init_acc_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_initial_access
    ADD CONSTRAINT cnstr_client_init_acc_pk PRIMARY KEY (id);


--
-- TOC entry 3790 (class 2606 OID 16862)
-- Name: realm_default_groups con_group_id_def_groups; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT con_group_id_def_groups UNIQUE (group_id);


--
-- TOC entry 3632 (class 2606 OID 16864)
-- Name: broker_link constr_broker_link_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.broker_link
    ADD CONSTRAINT constr_broker_link_pk PRIMARY KEY (identity_provider, user_id);


--
-- TOC entry 3675 (class 2606 OID 16866)
-- Name: client_user_session_note constr_cl_usr_ses_note; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_user_session_note
    ADD CONSTRAINT constr_cl_usr_ses_note PRIMARY KEY (client_session, name);


--
-- TOC entry 3681 (class 2606 OID 16868)
-- Name: component_config constr_component_config_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_config
    ADD CONSTRAINT constr_component_config_pk PRIMARY KEY (id);


--
-- TOC entry 3677 (class 2606 OID 16870)
-- Name: component constr_component_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component
    ADD CONSTRAINT constr_component_pk PRIMARY KEY (id);


--
-- TOC entry 3718 (class 2606 OID 16872)
-- Name: fed_user_required_action constr_fed_required_action; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fed_user_required_action
    ADD CONSTRAINT constr_fed_required_action PRIMARY KEY (required_action, user_id);


--
-- TOC entry 3700 (class 2606 OID 16874)
-- Name: fed_user_attribute constr_fed_user_attr_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fed_user_attribute
    ADD CONSTRAINT constr_fed_user_attr_pk PRIMARY KEY (id);


--
-- TOC entry 3703 (class 2606 OID 16876)
-- Name: fed_user_consent constr_fed_user_consent_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fed_user_consent
    ADD CONSTRAINT constr_fed_user_consent_pk PRIMARY KEY (id);


--
-- TOC entry 3710 (class 2606 OID 16878)
-- Name: fed_user_credential constr_fed_user_cred_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fed_user_credential
    ADD CONSTRAINT constr_fed_user_cred_pk PRIMARY KEY (id);


--
-- TOC entry 3714 (class 2606 OID 16880)
-- Name: fed_user_group_membership constr_fed_user_group; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fed_user_group_membership
    ADD CONSTRAINT constr_fed_user_group PRIMARY KEY (group_id, user_id);


--
-- TOC entry 3722 (class 2606 OID 16882)
-- Name: fed_user_role_mapping constr_fed_user_role; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fed_user_role_mapping
    ADD CONSTRAINT constr_fed_user_role PRIMARY KEY (role_id, user_id);


--
-- TOC entry 3730 (class 2606 OID 16884)
-- Name: federated_user constr_federated_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_user
    ADD CONSTRAINT constr_federated_user PRIMARY KEY (id);


--
-- TOC entry 3792 (class 2606 OID 16886)
-- Name: realm_default_groups constr_realm_default_groups; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT constr_realm_default_groups PRIMARY KEY (realm_id, group_id);


--
-- TOC entry 3795 (class 2606 OID 16888)
-- Name: realm_enabled_event_types constr_realm_enabl_event_types; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_enabled_event_types
    ADD CONSTRAINT constr_realm_enabl_event_types PRIMARY KEY (realm_id, value);


--
-- TOC entry 3798 (class 2606 OID 16890)
-- Name: realm_events_listeners constr_realm_events_listeners; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_events_listeners
    ADD CONSTRAINT constr_realm_events_listeners PRIMARY KEY (realm_id, value);


--
-- TOC entry 3807 (class 2606 OID 16892)
-- Name: realm_supported_locales constr_realm_supported_locales; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_supported_locales
    ADD CONSTRAINT constr_realm_supported_locales PRIMARY KEY (realm_id, value);


--
-- TOC entry 3739 (class 2606 OID 16894)
-- Name: identity_provider constraint_2b; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT constraint_2b PRIMARY KEY (internal_id);


--
-- TOC entry 3639 (class 2606 OID 16896)
-- Name: client_attributes constraint_3c; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_attributes
    ADD CONSTRAINT constraint_3c PRIMARY KEY (client_id, name);


--
-- TOC entry 3697 (class 2606 OID 16898)
-- Name: event_entity constraint_4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_entity
    ADD CONSTRAINT constraint_4 PRIMARY KEY (id);


--
-- TOC entry 3726 (class 2606 OID 16900)
-- Name: federated_identity constraint_40; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_identity
    ADD CONSTRAINT constraint_40 PRIMARY KEY (identity_provider, user_id);


--
-- TOC entry 3782 (class 2606 OID 16902)
-- Name: realm constraint_4a; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT constraint_4a PRIMARY KEY (id);


--
-- TOC entry 3673 (class 2606 OID 16904)
-- Name: client_session_role constraint_5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session_role
    ADD CONSTRAINT constraint_5 PRIMARY KEY (client_session, role_id);


--
-- TOC entry 3898 (class 2606 OID 16906)
-- Name: user_session constraint_57; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT constraint_57 PRIMARY KEY (id);


--
-- TOC entry 3886 (class 2606 OID 16908)
-- Name: user_federation_provider constraint_5c; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_provider
    ADD CONSTRAINT constraint_5c PRIMARY KEY (id);


--
-- TOC entry 3669 (class 2606 OID 16910)
-- Name: client_session_note constraint_5e; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session_note
    ADD CONSTRAINT constraint_5e PRIMARY KEY (client_session, name);


--
-- TOC entry 3634 (class 2606 OID 16912)
-- Name: client constraint_7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT constraint_7 PRIMARY KEY (id);


--
-- TOC entry 3664 (class 2606 OID 16914)
-- Name: client_session constraint_8; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session
    ADD CONSTRAINT constraint_8 PRIMARY KEY (id);


--
-- TOC entry 3852 (class 2606 OID 16916)
-- Name: scope_mapping constraint_81; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scope_mapping
    ADD CONSTRAINT constraint_81 PRIMARY KEY (client_id, role_id);


--
-- TOC entry 3646 (class 2606 OID 16918)
-- Name: client_node_registrations constraint_84; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_node_registrations
    ADD CONSTRAINT constraint_84 PRIMARY KEY (client_id, name);


--
-- TOC entry 3787 (class 2606 OID 16920)
-- Name: realm_attribute constraint_9; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_attribute
    ADD CONSTRAINT constraint_9 PRIMARY KEY (name, realm_id);


--
-- TOC entry 3803 (class 2606 OID 16922)
-- Name: realm_required_credential constraint_92; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_required_credential
    ADD CONSTRAINT constraint_92 PRIMARY KEY (realm_id, type);


--
-- TOC entry 3757 (class 2606 OID 16924)
-- Name: keycloak_role constraint_a; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT constraint_a PRIMARY KEY (id);


--
-- TOC entry 3614 (class 2606 OID 16926)
-- Name: admin_event_entity constraint_admin_event_entity; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admin_event_entity
    ADD CONSTRAINT constraint_admin_event_entity PRIMARY KEY (id);


--
-- TOC entry 3630 (class 2606 OID 16928)
-- Name: authenticator_config_entry constraint_auth_cfg_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authenticator_config_entry
    ADD CONSTRAINT constraint_auth_cfg_pk PRIMARY KEY (authenticator_id, name);


--
-- TOC entry 3620 (class 2606 OID 16930)
-- Name: authentication_execution constraint_auth_exec_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT constraint_auth_exec_pk PRIMARY KEY (id);


--
-- TOC entry 3624 (class 2606 OID 16932)
-- Name: authentication_flow constraint_auth_flow_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authentication_flow
    ADD CONSTRAINT constraint_auth_flow_pk PRIMARY KEY (id);


--
-- TOC entry 3627 (class 2606 OID 16934)
-- Name: authenticator_config constraint_auth_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authenticator_config
    ADD CONSTRAINT constraint_auth_pk PRIMARY KEY (id);


--
-- TOC entry 3667 (class 2606 OID 16936)
-- Name: client_session_auth_status constraint_auth_status_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session_auth_status
    ADD CONSTRAINT constraint_auth_status_pk PRIMARY KEY (client_session, authenticator);


--
-- TOC entry 3895 (class 2606 OID 16938)
-- Name: user_role_mapping constraint_c; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role_mapping
    ADD CONSTRAINT constraint_c PRIMARY KEY (role_id, user_id);


--
-- TOC entry 3684 (class 2606 OID 16940)
-- Name: composite_role constraint_composite_role; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT constraint_composite_role PRIMARY KEY (composite, child_role);


--
-- TOC entry 3671 (class 2606 OID 16942)
-- Name: client_session_prot_mapper constraint_cs_pmp_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session_prot_mapper
    ADD CONSTRAINT constraint_cs_pmp_pk PRIMARY KEY (client_session, protocol_mapper_id);


--
-- TOC entry 3744 (class 2606 OID 16944)
-- Name: identity_provider_config constraint_d; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_provider_config
    ADD CONSTRAINT constraint_d PRIMARY KEY (identity_provider_id, name);


--
-- TOC entry 3774 (class 2606 OID 16946)
-- Name: policy_config constraint_dpc; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.policy_config
    ADD CONSTRAINT constraint_dpc PRIMARY KEY (policy_id, name);


--
-- TOC entry 3805 (class 2606 OID 16948)
-- Name: realm_smtp_config constraint_e; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_smtp_config
    ADD CONSTRAINT constraint_e PRIMARY KEY (realm_id, name);


--
-- TOC entry 3688 (class 2606 OID 16950)
-- Name: credential constraint_f; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT constraint_f PRIMARY KEY (id);


--
-- TOC entry 3878 (class 2606 OID 16952)
-- Name: user_federation_config constraint_f9; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_config
    ADD CONSTRAINT constraint_f9 PRIMARY KEY (user_federation_provider_id, name);


--
-- TOC entry 3828 (class 2606 OID 16954)
-- Name: resource_server_perm_ticket constraint_fapmt; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT constraint_fapmt PRIMARY KEY (id);


--
-- TOC entry 3837 (class 2606 OID 16956)
-- Name: resource_server_resource constraint_farsr; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT constraint_farsr PRIMARY KEY (id);


--
-- TOC entry 3832 (class 2606 OID 16958)
-- Name: resource_server_policy constraint_farsrp; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT constraint_farsrp PRIMARY KEY (id);


--
-- TOC entry 3617 (class 2606 OID 16960)
-- Name: associated_policy constraint_farsrpap; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT constraint_farsrpap PRIMARY KEY (policy_id, associated_policy_id);


--
-- TOC entry 3820 (class 2606 OID 16962)
-- Name: resource_policy constraint_farsrpp; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT constraint_farsrpp PRIMARY KEY (resource_id, policy_id);


--
-- TOC entry 3842 (class 2606 OID 16964)
-- Name: resource_server_scope constraint_farsrs; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT constraint_farsrs PRIMARY KEY (id);


--
-- TOC entry 3823 (class 2606 OID 16966)
-- Name: resource_scope constraint_farsrsp; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT constraint_farsrsp PRIMARY KEY (resource_id, scope_id);


--
-- TOC entry 3855 (class 2606 OID 16968)
-- Name: scope_policy constraint_farsrsps; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT constraint_farsrsps PRIMARY KEY (scope_id, policy_id);


--
-- TOC entry 3870 (class 2606 OID 16970)
-- Name: user_entity constraint_fb; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT constraint_fb PRIMARY KEY (id);


--
-- TOC entry 3884 (class 2606 OID 16972)
-- Name: user_federation_mapper_config constraint_fedmapper_cfg_pm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_mapper_config
    ADD CONSTRAINT constraint_fedmapper_cfg_pm PRIMARY KEY (user_federation_mapper_id, name);


--
-- TOC entry 3880 (class 2606 OID 16974)
-- Name: user_federation_mapper constraint_fedmapperpm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT constraint_fedmapperpm PRIMARY KEY (id);


--
-- TOC entry 3708 (class 2606 OID 16976)
-- Name: fed_user_consent_cl_scope constraint_fgrntcsnt_clsc_pm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fed_user_consent_cl_scope
    ADD CONSTRAINT constraint_fgrntcsnt_clsc_pm PRIMARY KEY (user_consent_id, scope_id);


--
-- TOC entry 3867 (class 2606 OID 16978)
-- Name: user_consent_client_scope constraint_grntcsnt_clsc_pm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_consent_client_scope
    ADD CONSTRAINT constraint_grntcsnt_clsc_pm PRIMARY KEY (user_consent_id, scope_id);


--
-- TOC entry 3862 (class 2606 OID 16980)
-- Name: user_consent constraint_grntcsnt_pm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT constraint_grntcsnt_pm PRIMARY KEY (id);


--
-- TOC entry 3751 (class 2606 OID 16982)
-- Name: keycloak_group constraint_group; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.keycloak_group
    ADD CONSTRAINT constraint_group PRIMARY KEY (id);


--
-- TOC entry 3732 (class 2606 OID 16984)
-- Name: group_attribute constraint_group_attribute_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_attribute
    ADD CONSTRAINT constraint_group_attribute_pk PRIMARY KEY (id);


--
-- TOC entry 3736 (class 2606 OID 16986)
-- Name: group_role_mapping constraint_group_role; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_role_mapping
    ADD CONSTRAINT constraint_group_role PRIMARY KEY (role_id, group_id);


--
-- TOC entry 3746 (class 2606 OID 16988)
-- Name: identity_provider_mapper constraint_idpm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_provider_mapper
    ADD CONSTRAINT constraint_idpm PRIMARY KEY (id);


--
-- TOC entry 3749 (class 2606 OID 16990)
-- Name: idp_mapper_config constraint_idpmconfig; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.idp_mapper_config
    ADD CONSTRAINT constraint_idpmconfig PRIMARY KEY (idp_mapper_id, name);


--
-- TOC entry 3761 (class 2606 OID 16992)
-- Name: migration_model constraint_migmod; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.migration_model
    ADD CONSTRAINT constraint_migmod PRIMARY KEY (id);


--
-- TOC entry 3764 (class 2606 OID 16994)
-- Name: offline_client_session constraint_offl_cl_ses_pk3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.offline_client_session
    ADD CONSTRAINT constraint_offl_cl_ses_pk3 PRIMARY KEY (user_session_id, client_id, client_storage_provider, external_client_id, offline_flag);


--
-- TOC entry 3768 (class 2606 OID 16996)
-- Name: offline_user_session constraint_offl_us_ses_pk2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.offline_user_session
    ADD CONSTRAINT constraint_offl_us_ses_pk2 PRIMARY KEY (user_session_id, offline_flag);


--
-- TOC entry 3776 (class 2606 OID 16998)
-- Name: protocol_mapper constraint_pcm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT constraint_pcm PRIMARY KEY (id);


--
-- TOC entry 3780 (class 2606 OID 17000)
-- Name: protocol_mapper_config constraint_pmconfig; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.protocol_mapper_config
    ADD CONSTRAINT constraint_pmconfig PRIMARY KEY (protocol_mapper_id, name);


--
-- TOC entry 3810 (class 2606 OID 17002)
-- Name: redirect_uris constraint_redirect_uris; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.redirect_uris
    ADD CONSTRAINT constraint_redirect_uris PRIMARY KEY (client_id, value);


--
-- TOC entry 3813 (class 2606 OID 17004)
-- Name: required_action_config constraint_req_act_cfg_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.required_action_config
    ADD CONSTRAINT constraint_req_act_cfg_pk PRIMARY KEY (required_action_id, name);


--
-- TOC entry 3815 (class 2606 OID 17006)
-- Name: required_action_provider constraint_req_act_prv_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.required_action_provider
    ADD CONSTRAINT constraint_req_act_prv_pk PRIMARY KEY (id);


--
-- TOC entry 3892 (class 2606 OID 17008)
-- Name: user_required_action constraint_required_action; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_required_action
    ADD CONSTRAINT constraint_required_action PRIMARY KEY (required_action, user_id);


--
-- TOC entry 3847 (class 2606 OID 17010)
-- Name: resource_uris constraint_resour_uris_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_uris
    ADD CONSTRAINT constraint_resour_uris_pk PRIMARY KEY (resource_id, value);


--
-- TOC entry 3849 (class 2606 OID 17012)
-- Name: role_attribute constraint_role_attribute_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_attribute
    ADD CONSTRAINT constraint_role_attribute_pk PRIMARY KEY (id);


--
-- TOC entry 3858 (class 2606 OID 17014)
-- Name: user_attribute constraint_user_attribute_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT constraint_user_attribute_pk PRIMARY KEY (id);


--
-- TOC entry 3889 (class 2606 OID 17016)
-- Name: user_group_membership constraint_user_group; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_group_membership
    ADD CONSTRAINT constraint_user_group PRIMARY KEY (group_id, user_id);


--
-- TOC entry 3900 (class 2606 OID 17018)
-- Name: user_session_note constraint_usn_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session_note
    ADD CONSTRAINT constraint_usn_pk PRIMARY KEY (user_session, name);


--
-- TOC entry 3904 (class 2606 OID 17020)
-- Name: web_origins constraint_web_origins; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.web_origins
    ADD CONSTRAINT constraint_web_origins PRIMARY KEY (client_id, value);


--
-- TOC entry 3691 (class 2606 OID 17022)
-- Name: databasechangeloglock databasechangeloglock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.databasechangeloglock
    ADD CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id);


--
-- TOC entry 3654 (class 2606 OID 17024)
-- Name: client_scope_attributes pk_cl_tmpl_attr; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_scope_attributes
    ADD CONSTRAINT pk_cl_tmpl_attr PRIMARY KEY (scope_id, name);


--
-- TOC entry 3649 (class 2606 OID 17026)
-- Name: client_scope pk_cli_template; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_scope
    ADD CONSTRAINT pk_cli_template PRIMARY KEY (id);


--
-- TOC entry 3826 (class 2606 OID 17028)
-- Name: resource_server pk_resource_server; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server
    ADD CONSTRAINT pk_resource_server PRIMARY KEY (id);


--
-- TOC entry 3662 (class 2606 OID 17030)
-- Name: client_scope_role_mapping pk_template_scope; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_scope_role_mapping
    ADD CONSTRAINT pk_template_scope PRIMARY KEY (scope_id, role_id);


--
-- TOC entry 3695 (class 2606 OID 17032)
-- Name: default_client_scope r_def_cli_scope_bind; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.default_client_scope
    ADD CONSTRAINT r_def_cli_scope_bind PRIMARY KEY (realm_id, scope_id);


--
-- TOC entry 3801 (class 2606 OID 17034)
-- Name: realm_localizations realm_localizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_localizations
    ADD CONSTRAINT realm_localizations_pkey PRIMARY KEY (realm_id, locale);


--
-- TOC entry 3818 (class 2606 OID 17036)
-- Name: resource_attribute res_attr_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_attribute
    ADD CONSTRAINT res_attr_pk PRIMARY KEY (id);


--
-- TOC entry 3753 (class 2606 OID 17038)
-- Name: keycloak_group sibling_names; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.keycloak_group
    ADD CONSTRAINT sibling_names UNIQUE (realm_id, parent_group, name);


--
-- TOC entry 3742 (class 2606 OID 17040)
-- Name: identity_provider uk_2daelwnibji49avxsrtuf6xj33; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT uk_2daelwnibji49avxsrtuf6xj33 UNIQUE (provider_alias, realm_id);


--
-- TOC entry 3637 (class 2606 OID 17042)
-- Name: client uk_b71cjlbenv945rb6gcon438at; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT uk_b71cjlbenv945rb6gcon438at UNIQUE (realm_id, client_id);


--
-- TOC entry 3651 (class 2606 OID 17044)
-- Name: client_scope uk_cli_scope; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_scope
    ADD CONSTRAINT uk_cli_scope UNIQUE (realm_id, name);


--
-- TOC entry 3874 (class 2606 OID 17046)
-- Name: user_entity uk_dykn684sl8up1crfei6eckhd7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT uk_dykn684sl8up1crfei6eckhd7 UNIQUE (realm_id, email_constraint);


--
-- TOC entry 3840 (class 2606 OID 17048)
-- Name: resource_server_resource uk_frsr6t700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT uk_frsr6t700s9v50bu18ws5ha6 UNIQUE (name, owner, resource_server_id);


--
-- TOC entry 3830 (class 2606 OID 17050)
-- Name: resource_server_perm_ticket uk_frsr6t700s9v50bu18ws5pmt; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT uk_frsr6t700s9v50bu18ws5pmt UNIQUE (owner, requester, resource_server_id, resource_id, scope_id);


--
-- TOC entry 3835 (class 2606 OID 17052)
-- Name: resource_server_policy uk_frsrpt700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT uk_frsrpt700s9v50bu18ws5ha6 UNIQUE (name, resource_server_id);


--
-- TOC entry 3845 (class 2606 OID 17054)
-- Name: resource_server_scope uk_frsrst700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT uk_frsrst700s9v50bu18ws5ha6 UNIQUE (name, resource_server_id);


--
-- TOC entry 3865 (class 2606 OID 17056)
-- Name: user_consent uk_jkuwuvd56ontgsuhogm8uewrt; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT uk_jkuwuvd56ontgsuhogm8uewrt UNIQUE (client_id, client_storage_provider, external_client_id, user_id);


--
-- TOC entry 3785 (class 2606 OID 17058)
-- Name: realm uk_orvsdmla56612eaefiq6wl5oi; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT uk_orvsdmla56612eaefiq6wl5oi UNIQUE (name);


--
-- TOC entry 3876 (class 2606 OID 17060)
-- Name: user_entity uk_ru8tt6t700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT uk_ru8tt6t700s9v50bu18ws5ha6 UNIQUE (realm_id, username);


--
-- TOC entry 3615 (class 1259 OID 17061)
-- Name: idx_admin_event_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_admin_event_time ON public.admin_event_entity USING btree (realm_id, admin_event_time);


--
-- TOC entry 3618 (class 1259 OID 17062)
-- Name: idx_assoc_pol_assoc_pol_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assoc_pol_assoc_pol_id ON public.associated_policy USING btree (associated_policy_id);


--
-- TOC entry 3628 (class 1259 OID 17063)
-- Name: idx_auth_config_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_config_realm ON public.authenticator_config USING btree (realm_id);


--
-- TOC entry 3621 (class 1259 OID 17064)
-- Name: idx_auth_exec_flow; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_exec_flow ON public.authentication_execution USING btree (flow_id);


--
-- TOC entry 3622 (class 1259 OID 17065)
-- Name: idx_auth_exec_realm_flow; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_exec_realm_flow ON public.authentication_execution USING btree (realm_id, flow_id);


--
-- TOC entry 3625 (class 1259 OID 17066)
-- Name: idx_auth_flow_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_flow_realm ON public.authentication_flow USING btree (realm_id);


--
-- TOC entry 3657 (class 1259 OID 17067)
-- Name: idx_cl_clscope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cl_clscope ON public.client_scope_client USING btree (scope_id);


--
-- TOC entry 3635 (class 1259 OID 17068)
-- Name: idx_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_id ON public.client USING btree (client_id);


--
-- TOC entry 3644 (class 1259 OID 17069)
-- Name: idx_client_init_acc_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_init_acc_realm ON public.client_initial_access USING btree (realm_id);


--
-- TOC entry 3665 (class 1259 OID 17070)
-- Name: idx_client_session_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_session_session ON public.client_session USING btree (session_id);


--
-- TOC entry 3652 (class 1259 OID 17071)
-- Name: idx_clscope_attrs; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clscope_attrs ON public.client_scope_attributes USING btree (scope_id);


--
-- TOC entry 3658 (class 1259 OID 17072)
-- Name: idx_clscope_cl; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clscope_cl ON public.client_scope_client USING btree (client_id);


--
-- TOC entry 3777 (class 1259 OID 17073)
-- Name: idx_clscope_protmap; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clscope_protmap ON public.protocol_mapper USING btree (client_scope_id);


--
-- TOC entry 3659 (class 1259 OID 17074)
-- Name: idx_clscope_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clscope_role ON public.client_scope_role_mapping USING btree (scope_id);


--
-- TOC entry 3682 (class 1259 OID 17075)
-- Name: idx_compo_config_compo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compo_config_compo ON public.component_config USING btree (component_id);


--
-- TOC entry 3678 (class 1259 OID 17076)
-- Name: idx_component_provider_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_component_provider_type ON public.component USING btree (provider_type);


--
-- TOC entry 3679 (class 1259 OID 17077)
-- Name: idx_component_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_component_realm ON public.component USING btree (realm_id);


--
-- TOC entry 3685 (class 1259 OID 17078)
-- Name: idx_composite; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_composite ON public.composite_role USING btree (composite);


--
-- TOC entry 3686 (class 1259 OID 17079)
-- Name: idx_composite_child; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_composite_child ON public.composite_role USING btree (child_role);


--
-- TOC entry 3692 (class 1259 OID 17080)
-- Name: idx_defcls_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_defcls_realm ON public.default_client_scope USING btree (realm_id);


--
-- TOC entry 3693 (class 1259 OID 17081)
-- Name: idx_defcls_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_defcls_scope ON public.default_client_scope USING btree (scope_id);


--
-- TOC entry 3698 (class 1259 OID 17082)
-- Name: idx_event_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_time ON public.event_entity USING btree (realm_id, event_time);


--
-- TOC entry 3727 (class 1259 OID 17083)
-- Name: idx_fedidentity_feduser; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fedidentity_feduser ON public.federated_identity USING btree (federated_user_id);


--
-- TOC entry 3728 (class 1259 OID 17084)
-- Name: idx_fedidentity_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fedidentity_user ON public.federated_identity USING btree (user_id);


--
-- TOC entry 3701 (class 1259 OID 17085)
-- Name: idx_fu_attribute; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_attribute ON public.fed_user_attribute USING btree (user_id, realm_id, name);


--
-- TOC entry 3704 (class 1259 OID 17086)
-- Name: idx_fu_cnsnt_ext; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_cnsnt_ext ON public.fed_user_consent USING btree (user_id, client_storage_provider, external_client_id);


--
-- TOC entry 3705 (class 1259 OID 17087)
-- Name: idx_fu_consent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_consent ON public.fed_user_consent USING btree (user_id, client_id);


--
-- TOC entry 3706 (class 1259 OID 17088)
-- Name: idx_fu_consent_ru; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_consent_ru ON public.fed_user_consent USING btree (realm_id, user_id);


--
-- TOC entry 3711 (class 1259 OID 17089)
-- Name: idx_fu_credential; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_credential ON public.fed_user_credential USING btree (user_id, type);


--
-- TOC entry 3712 (class 1259 OID 17090)
-- Name: idx_fu_credential_ru; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_credential_ru ON public.fed_user_credential USING btree (realm_id, user_id);


--
-- TOC entry 3715 (class 1259 OID 17091)
-- Name: idx_fu_group_membership; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_group_membership ON public.fed_user_group_membership USING btree (user_id, group_id);


--
-- TOC entry 3716 (class 1259 OID 17092)
-- Name: idx_fu_group_membership_ru; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_group_membership_ru ON public.fed_user_group_membership USING btree (realm_id, user_id);


--
-- TOC entry 3719 (class 1259 OID 17093)
-- Name: idx_fu_required_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_required_action ON public.fed_user_required_action USING btree (user_id, required_action);


--
-- TOC entry 3720 (class 1259 OID 17094)
-- Name: idx_fu_required_action_ru; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_required_action_ru ON public.fed_user_required_action USING btree (realm_id, user_id);


--
-- TOC entry 3723 (class 1259 OID 17095)
-- Name: idx_fu_role_mapping; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_role_mapping ON public.fed_user_role_mapping USING btree (user_id, role_id);


--
-- TOC entry 3724 (class 1259 OID 17096)
-- Name: idx_fu_role_mapping_ru; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fu_role_mapping_ru ON public.fed_user_role_mapping USING btree (realm_id, user_id);


--
-- TOC entry 3733 (class 1259 OID 17097)
-- Name: idx_group_att_by_name_value; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_group_att_by_name_value ON public.group_attribute USING btree (name, ((value)::character varying(250)));


--
-- TOC entry 3734 (class 1259 OID 17098)
-- Name: idx_group_attr_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_group_attr_group ON public.group_attribute USING btree (group_id);


--
-- TOC entry 3737 (class 1259 OID 17099)
-- Name: idx_group_role_mapp_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_group_role_mapp_group ON public.group_role_mapping USING btree (group_id);


--
-- TOC entry 3747 (class 1259 OID 17100)
-- Name: idx_id_prov_mapp_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_id_prov_mapp_realm ON public.identity_provider_mapper USING btree (realm_id);


--
-- TOC entry 3740 (class 1259 OID 17101)
-- Name: idx_ident_prov_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ident_prov_realm ON public.identity_provider USING btree (realm_id);


--
-- TOC entry 3758 (class 1259 OID 17102)
-- Name: idx_keycloak_role_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_keycloak_role_client ON public.keycloak_role USING btree (client);


--
-- TOC entry 3759 (class 1259 OID 17103)
-- Name: idx_keycloak_role_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_keycloak_role_realm ON public.keycloak_role USING btree (realm);


--
-- TOC entry 3765 (class 1259 OID 17104)
-- Name: idx_offline_css_preload; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_offline_css_preload ON public.offline_client_session USING btree (client_id, offline_flag);


--
-- TOC entry 3769 (class 1259 OID 17105)
-- Name: idx_offline_uss_by_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_offline_uss_by_user ON public.offline_user_session USING btree (user_id, realm_id, offline_flag);


--
-- TOC entry 3770 (class 1259 OID 17106)
-- Name: idx_offline_uss_by_usersess; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_offline_uss_by_usersess ON public.offline_user_session USING btree (realm_id, offline_flag, user_session_id);


--
-- TOC entry 3771 (class 1259 OID 17107)
-- Name: idx_offline_uss_createdon; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_offline_uss_createdon ON public.offline_user_session USING btree (created_on);


--
-- TOC entry 3772 (class 1259 OID 17108)
-- Name: idx_offline_uss_preload; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_offline_uss_preload ON public.offline_user_session USING btree (offline_flag, created_on, user_session_id);


--
-- TOC entry 3778 (class 1259 OID 17109)
-- Name: idx_protocol_mapper_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_protocol_mapper_client ON public.protocol_mapper USING btree (client_id);


--
-- TOC entry 3788 (class 1259 OID 17110)
-- Name: idx_realm_attr_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_realm_attr_realm ON public.realm_attribute USING btree (realm_id);


--
-- TOC entry 3647 (class 1259 OID 17111)
-- Name: idx_realm_clscope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_realm_clscope ON public.client_scope USING btree (realm_id);


--
-- TOC entry 3793 (class 1259 OID 17112)
-- Name: idx_realm_def_grp_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_realm_def_grp_realm ON public.realm_default_groups USING btree (realm_id);


--
-- TOC entry 3799 (class 1259 OID 17113)
-- Name: idx_realm_evt_list_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_realm_evt_list_realm ON public.realm_events_listeners USING btree (realm_id);


--
-- TOC entry 3796 (class 1259 OID 17114)
-- Name: idx_realm_evt_types_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_realm_evt_types_realm ON public.realm_enabled_event_types USING btree (realm_id);


--
-- TOC entry 3783 (class 1259 OID 17115)
-- Name: idx_realm_master_adm_cli; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_realm_master_adm_cli ON public.realm USING btree (master_admin_client);


--
-- TOC entry 3808 (class 1259 OID 17116)
-- Name: idx_realm_supp_local_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_realm_supp_local_realm ON public.realm_supported_locales USING btree (realm_id);


--
-- TOC entry 3811 (class 1259 OID 17117)
-- Name: idx_redir_uri_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_redir_uri_client ON public.redirect_uris USING btree (client_id);


--
-- TOC entry 3816 (class 1259 OID 17118)
-- Name: idx_req_act_prov_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_req_act_prov_realm ON public.required_action_provider USING btree (realm_id);


--
-- TOC entry 3821 (class 1259 OID 17119)
-- Name: idx_res_policy_policy; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_res_policy_policy ON public.resource_policy USING btree (policy_id);


--
-- TOC entry 3824 (class 1259 OID 17120)
-- Name: idx_res_scope_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_res_scope_scope ON public.resource_scope USING btree (scope_id);


--
-- TOC entry 3833 (class 1259 OID 17121)
-- Name: idx_res_serv_pol_res_serv; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_res_serv_pol_res_serv ON public.resource_server_policy USING btree (resource_server_id);


--
-- TOC entry 3838 (class 1259 OID 17122)
-- Name: idx_res_srv_res_res_srv; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_res_srv_res_res_srv ON public.resource_server_resource USING btree (resource_server_id);


--
-- TOC entry 3843 (class 1259 OID 17123)
-- Name: idx_res_srv_scope_res_srv; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_res_srv_scope_res_srv ON public.resource_server_scope USING btree (resource_server_id);


--
-- TOC entry 3850 (class 1259 OID 17124)
-- Name: idx_role_attribute; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_attribute ON public.role_attribute USING btree (role_id);


--
-- TOC entry 3660 (class 1259 OID 17125)
-- Name: idx_role_clscope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_clscope ON public.client_scope_role_mapping USING btree (role_id);


--
-- TOC entry 3853 (class 1259 OID 17126)
-- Name: idx_scope_mapping_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scope_mapping_role ON public.scope_mapping USING btree (role_id);


--
-- TOC entry 3856 (class 1259 OID 17127)
-- Name: idx_scope_policy_policy; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scope_policy_policy ON public.scope_policy USING btree (policy_id);


--
-- TOC entry 3762 (class 1259 OID 17128)
-- Name: idx_update_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_update_time ON public.migration_model USING btree (update_time);


--
-- TOC entry 3766 (class 1259 OID 17129)
-- Name: idx_us_sess_id_on_cl_sess; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_us_sess_id_on_cl_sess ON public.offline_client_session USING btree (user_session_id);


--
-- TOC entry 3868 (class 1259 OID 17130)
-- Name: idx_usconsent_clscope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usconsent_clscope ON public.user_consent_client_scope USING btree (user_consent_id);


--
-- TOC entry 3859 (class 1259 OID 17131)
-- Name: idx_user_attribute; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_attribute ON public.user_attribute USING btree (user_id);


--
-- TOC entry 3860 (class 1259 OID 17132)
-- Name: idx_user_attribute_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_attribute_name ON public.user_attribute USING btree (name, value);


--
-- TOC entry 3863 (class 1259 OID 17133)
-- Name: idx_user_consent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_consent ON public.user_consent USING btree (user_id);


--
-- TOC entry 3689 (class 1259 OID 17134)
-- Name: idx_user_credential; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_credential ON public.credential USING btree (user_id);


--
-- TOC entry 3871 (class 1259 OID 17135)
-- Name: idx_user_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_email ON public.user_entity USING btree (email);


--
-- TOC entry 3890 (class 1259 OID 17136)
-- Name: idx_user_group_mapping; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_group_mapping ON public.user_group_membership USING btree (user_id);


--
-- TOC entry 3893 (class 1259 OID 17137)
-- Name: idx_user_reqactions; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_reqactions ON public.user_required_action USING btree (user_id);


--
-- TOC entry 3896 (class 1259 OID 17138)
-- Name: idx_user_role_mapping; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_role_mapping ON public.user_role_mapping USING btree (user_id);


--
-- TOC entry 3872 (class 1259 OID 17139)
-- Name: idx_user_service_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_service_account ON public.user_entity USING btree (realm_id, service_account_client_link);


--
-- TOC entry 3881 (class 1259 OID 17140)
-- Name: idx_usr_fed_map_fed_prv; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usr_fed_map_fed_prv ON public.user_federation_mapper USING btree (federation_provider_id);


--
-- TOC entry 3882 (class 1259 OID 17141)
-- Name: idx_usr_fed_map_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usr_fed_map_realm ON public.user_federation_mapper USING btree (realm_id);


--
-- TOC entry 3887 (class 1259 OID 17142)
-- Name: idx_usr_fed_prv_realm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usr_fed_prv_realm ON public.user_federation_provider USING btree (realm_id);


--
-- TOC entry 3905 (class 1259 OID 17143)
-- Name: idx_web_orig_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_web_orig_client ON public.web_origins USING btree (client_id);


--
-- TOC entry 3918 (class 2606 OID 17144)
-- Name: client_session_auth_status auth_status_constraint; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session_auth_status
    ADD CONSTRAINT auth_status_constraint FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3932 (class 2606 OID 17149)
-- Name: identity_provider fk2b4ebc52ae5c3b34; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT fk2b4ebc52ae5c3b34 FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3912 (class 2606 OID 17154)
-- Name: client_attributes fk3c47c64beacca966; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_attributes
    ADD CONSTRAINT fk3c47c64beacca966 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3929 (class 2606 OID 17159)
-- Name: federated_identity fk404288b92ef007a6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.federated_identity
    ADD CONSTRAINT fk404288b92ef007a6 FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3914 (class 2606 OID 17164)
-- Name: client_node_registrations fk4129723ba992f594; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_node_registrations
    ADD CONSTRAINT fk4129723ba992f594 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3919 (class 2606 OID 17169)
-- Name: client_session_note fk5edfb00ff51c2736; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session_note
    ADD CONSTRAINT fk5edfb00ff51c2736 FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3978 (class 2606 OID 17174)
-- Name: user_session_note fk5edfb00ff51d3472; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session_note
    ADD CONSTRAINT fk5edfb00ff51d3472 FOREIGN KEY (user_session) REFERENCES public.user_session(id);


--
-- TOC entry 3921 (class 2606 OID 17179)
-- Name: client_session_role fk_11b7sgqw18i532811v7o2dv76; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session_role
    ADD CONSTRAINT fk_11b7sgqw18i532811v7o2dv76 FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3948 (class 2606 OID 17184)
-- Name: redirect_uris fk_1burs8pb4ouj97h5wuppahv9f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.redirect_uris
    ADD CONSTRAINT fk_1burs8pb4ouj97h5wuppahv9f FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3974 (class 2606 OID 17189)
-- Name: user_federation_provider fk_1fj32f6ptolw2qy60cd8n01e8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_provider
    ADD CONSTRAINT fk_1fj32f6ptolw2qy60cd8n01e8 FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3920 (class 2606 OID 17194)
-- Name: client_session_prot_mapper fk_33a8sgqw18i532811v7o2dk89; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session_prot_mapper
    ADD CONSTRAINT fk_33a8sgqw18i532811v7o2dk89 FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3945 (class 2606 OID 17199)
-- Name: realm_required_credential fk_5hg65lybevavkqfki3kponh9v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_required_credential
    ADD CONSTRAINT fk_5hg65lybevavkqfki3kponh9v FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3950 (class 2606 OID 17204)
-- Name: resource_attribute fk_5hrm2vlf9ql5fu022kqepovbr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_attribute
    ADD CONSTRAINT fk_5hrm2vlf9ql5fu022kqepovbr FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3967 (class 2606 OID 17209)
-- Name: user_attribute fk_5hrm2vlf9ql5fu043kqepovbr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT fk_5hrm2vlf9ql5fu043kqepovbr FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3976 (class 2606 OID 17214)
-- Name: user_required_action fk_6qj3w1jw9cvafhe19bwsiuvmd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_required_action
    ADD CONSTRAINT fk_6qj3w1jw9cvafhe19bwsiuvmd FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3936 (class 2606 OID 17219)
-- Name: keycloak_role fk_6vyqfe4cn4wlq8r6kt5vdsj5c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT fk_6vyqfe4cn4wlq8r6kt5vdsj5c FOREIGN KEY (realm) REFERENCES public.realm(id);


--
-- TOC entry 3946 (class 2606 OID 17224)
-- Name: realm_smtp_config fk_70ej8xdxgxd0b9hh6180irr0o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_smtp_config
    ADD CONSTRAINT fk_70ej8xdxgxd0b9hh6180irr0o FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3941 (class 2606 OID 17229)
-- Name: realm_attribute fk_8shxd6l3e9atqukacxgpffptw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_attribute
    ADD CONSTRAINT fk_8shxd6l3e9atqukacxgpffptw FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3925 (class 2606 OID 17234)
-- Name: composite_role fk_a63wvekftu8jo1pnj81e7mce2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT fk_a63wvekftu8jo1pnj81e7mce2 FOREIGN KEY (composite) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3908 (class 2606 OID 17239)
-- Name: authentication_execution fk_auth_exec_flow; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT fk_auth_exec_flow FOREIGN KEY (flow_id) REFERENCES public.authentication_flow(id);


--
-- TOC entry 3909 (class 2606 OID 17244)
-- Name: authentication_execution fk_auth_exec_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT fk_auth_exec_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3910 (class 2606 OID 17249)
-- Name: authentication_flow fk_auth_flow_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authentication_flow
    ADD CONSTRAINT fk_auth_flow_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3911 (class 2606 OID 17254)
-- Name: authenticator_config fk_auth_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authenticator_config
    ADD CONSTRAINT fk_auth_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3917 (class 2606 OID 17259)
-- Name: client_session fk_b4ao2vcvat6ukau74wbwtfqo1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_session
    ADD CONSTRAINT fk_b4ao2vcvat6ukau74wbwtfqo1 FOREIGN KEY (session_id) REFERENCES public.user_session(id);


--
-- TOC entry 3977 (class 2606 OID 17264)
-- Name: user_role_mapping fk_c4fqv34p1mbylloxang7b1q3l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role_mapping
    ADD CONSTRAINT fk_c4fqv34p1mbylloxang7b1q3l FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3915 (class 2606 OID 17269)
-- Name: client_scope_attributes fk_cl_scope_attr_scope; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_scope_attributes
    ADD CONSTRAINT fk_cl_scope_attr_scope FOREIGN KEY (scope_id) REFERENCES public.client_scope(id);


--
-- TOC entry 3916 (class 2606 OID 17274)
-- Name: client_scope_role_mapping fk_cl_scope_rm_scope; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_scope_role_mapping
    ADD CONSTRAINT fk_cl_scope_rm_scope FOREIGN KEY (scope_id) REFERENCES public.client_scope(id);


--
-- TOC entry 3922 (class 2606 OID 17279)
-- Name: client_user_session_note fk_cl_usr_ses_note; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_user_session_note
    ADD CONSTRAINT fk_cl_usr_ses_note FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3938 (class 2606 OID 17284)
-- Name: protocol_mapper fk_cli_scope_mapper; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT fk_cli_scope_mapper FOREIGN KEY (client_scope_id) REFERENCES public.client_scope(id);


--
-- TOC entry 3913 (class 2606 OID 17289)
-- Name: client_initial_access fk_client_init_acc_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_initial_access
    ADD CONSTRAINT fk_client_init_acc_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3924 (class 2606 OID 17294)
-- Name: component_config fk_component_config; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_config
    ADD CONSTRAINT fk_component_config FOREIGN KEY (component_id) REFERENCES public.component(id);


--
-- TOC entry 3923 (class 2606 OID 17299)
-- Name: component fk_component_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component
    ADD CONSTRAINT fk_component_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3942 (class 2606 OID 17304)
-- Name: realm_default_groups fk_def_groups_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT fk_def_groups_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3973 (class 2606 OID 17309)
-- Name: user_federation_mapper_config fk_fedmapper_cfg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_mapper_config
    ADD CONSTRAINT fk_fedmapper_cfg FOREIGN KEY (user_federation_mapper_id) REFERENCES public.user_federation_mapper(id);


--
-- TOC entry 3971 (class 2606 OID 17314)
-- Name: user_federation_mapper fk_fedmapperpm_fedprv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT fk_fedmapperpm_fedprv FOREIGN KEY (federation_provider_id) REFERENCES public.user_federation_provider(id);


--
-- TOC entry 3972 (class 2606 OID 17319)
-- Name: user_federation_mapper fk_fedmapperpm_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT fk_fedmapperpm_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3906 (class 2606 OID 17324)
-- Name: associated_policy fk_frsr5s213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT fk_frsr5s213xcx4wnkog82ssrfy FOREIGN KEY (associated_policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3965 (class 2606 OID 17329)
-- Name: scope_policy fk_frsrasp13xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT fk_frsrasp13xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3955 (class 2606 OID 17334)
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog82sspmt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog82sspmt FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- TOC entry 3960 (class 2606 OID 17339)
-- Name: resource_server_resource fk_frsrho213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT fk_frsrho213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- TOC entry 3956 (class 2606 OID 17344)
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog83sspmt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog83sspmt FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3957 (class 2606 OID 17349)
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog84sspmt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog84sspmt FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- TOC entry 3907 (class 2606 OID 17354)
-- Name: associated_policy fk_frsrpas14xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT fk_frsrpas14xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3966 (class 2606 OID 17359)
-- Name: scope_policy fk_frsrpass3xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT fk_frsrpass3xcx4wnkog82ssrfy FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- TOC entry 3958 (class 2606 OID 17364)
-- Name: resource_server_perm_ticket fk_frsrpo2128cx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrpo2128cx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3959 (class 2606 OID 17369)
-- Name: resource_server_policy fk_frsrpo213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT fk_frsrpo213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- TOC entry 3953 (class 2606 OID 17374)
-- Name: resource_scope fk_frsrpos13xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT fk_frsrpos13xcx4wnkog82ssrfy FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3951 (class 2606 OID 17379)
-- Name: resource_policy fk_frsrpos53xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT fk_frsrpos53xcx4wnkog82ssrfy FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3952 (class 2606 OID 17384)
-- Name: resource_policy fk_frsrpp213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT fk_frsrpp213xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3954 (class 2606 OID 17389)
-- Name: resource_scope fk_frsrps213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT fk_frsrps213xcx4wnkog82ssrfy FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- TOC entry 3961 (class 2606 OID 17394)
-- Name: resource_server_scope fk_frsrso213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT fk_frsrso213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- TOC entry 3926 (class 2606 OID 17399)
-- Name: composite_role fk_gr7thllb9lu8q4vqa4524jjy8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT fk_gr7thllb9lu8q4vqa4524jjy8 FOREIGN KEY (child_role) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3969 (class 2606 OID 17404)
-- Name: user_consent_client_scope fk_grntcsnt_clsc_usc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_consent_client_scope
    ADD CONSTRAINT fk_grntcsnt_clsc_usc FOREIGN KEY (user_consent_id) REFERENCES public.user_consent(id);


--
-- TOC entry 3968 (class 2606 OID 17409)
-- Name: user_consent fk_grntcsnt_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT fk_grntcsnt_user FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3930 (class 2606 OID 17414)
-- Name: group_attribute fk_group_attribute_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_attribute
    ADD CONSTRAINT fk_group_attribute_group FOREIGN KEY (group_id) REFERENCES public.keycloak_group(id);


--
-- TOC entry 3931 (class 2606 OID 17419)
-- Name: group_role_mapping fk_group_role_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_role_mapping
    ADD CONSTRAINT fk_group_role_group FOREIGN KEY (group_id) REFERENCES public.keycloak_group(id);


--
-- TOC entry 3943 (class 2606 OID 17424)
-- Name: realm_enabled_event_types fk_h846o4h0w8epx5nwedrf5y69j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_enabled_event_types
    ADD CONSTRAINT fk_h846o4h0w8epx5nwedrf5y69j FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3944 (class 2606 OID 17429)
-- Name: realm_events_listeners fk_h846o4h0w8epx5nxev9f5y69j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_events_listeners
    ADD CONSTRAINT fk_h846o4h0w8epx5nxev9f5y69j FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3934 (class 2606 OID 17434)
-- Name: identity_provider_mapper fk_idpm_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_provider_mapper
    ADD CONSTRAINT fk_idpm_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3935 (class 2606 OID 17439)
-- Name: idp_mapper_config fk_idpmconfig; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.idp_mapper_config
    ADD CONSTRAINT fk_idpmconfig FOREIGN KEY (idp_mapper_id) REFERENCES public.identity_provider_mapper(id);


--
-- TOC entry 3979 (class 2606 OID 17444)
-- Name: web_origins fk_lojpho213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.web_origins
    ADD CONSTRAINT fk_lojpho213xcx4wnkog82ssrfy FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3964 (class 2606 OID 17449)
-- Name: scope_mapping fk_ouse064plmlr732lxjcn1q5f1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scope_mapping
    ADD CONSTRAINT fk_ouse064plmlr732lxjcn1q5f1 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3939 (class 2606 OID 17454)
-- Name: protocol_mapper fk_pcm_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT fk_pcm_realm FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3927 (class 2606 OID 17459)
-- Name: credential fk_pfyr0glasqyl0dei3kl69r6v0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT fk_pfyr0glasqyl0dei3kl69r6v0 FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3940 (class 2606 OID 17464)
-- Name: protocol_mapper_config fk_pmconfig; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.protocol_mapper_config
    ADD CONSTRAINT fk_pmconfig FOREIGN KEY (protocol_mapper_id) REFERENCES public.protocol_mapper(id);


--
-- TOC entry 3928 (class 2606 OID 17469)
-- Name: default_client_scope fk_r_def_cli_scope_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.default_client_scope
    ADD CONSTRAINT fk_r_def_cli_scope_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3949 (class 2606 OID 17474)
-- Name: required_action_provider fk_req_act_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.required_action_provider
    ADD CONSTRAINT fk_req_act_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3962 (class 2606 OID 17479)
-- Name: resource_uris fk_resource_server_uris; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_uris
    ADD CONSTRAINT fk_resource_server_uris FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3963 (class 2606 OID 17484)
-- Name: role_attribute fk_role_attribute_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_attribute
    ADD CONSTRAINT fk_role_attribute_id FOREIGN KEY (role_id) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3947 (class 2606 OID 17489)
-- Name: realm_supported_locales fk_supported_locales_realm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_supported_locales
    ADD CONSTRAINT fk_supported_locales_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3970 (class 2606 OID 17494)
-- Name: user_federation_config fk_t13hpu1j94r2ebpekr39x5eu5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_federation_config
    ADD CONSTRAINT fk_t13hpu1j94r2ebpekr39x5eu5 FOREIGN KEY (user_federation_provider_id) REFERENCES public.user_federation_provider(id);


--
-- TOC entry 3975 (class 2606 OID 17499)
-- Name: user_group_membership fk_user_group_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_group_membership
    ADD CONSTRAINT fk_user_group_user FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3937 (class 2606 OID 17504)
-- Name: policy_config fkdc34197cf864c4e43; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.policy_config
    ADD CONSTRAINT fkdc34197cf864c4e43 FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3933 (class 2606 OID 17509)
-- Name: identity_provider_config fkdc4897cf864c4e43; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_provider_config
    ADD CONSTRAINT fkdc4897cf864c4e43 FOREIGN KEY (identity_provider_id) REFERENCES public.identity_provider(internal_id);


-- Completed on 2023-05-19 13:52:11 UTC

--
-- PostgreSQL database dump complete
--

