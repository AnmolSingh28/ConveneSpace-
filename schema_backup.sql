--
-- PostgreSQL database dump
--

\restrict Xe9ANpAagQuYQH02ur9hUBeupyTZMRsA6CoDLgzNMQJswRMWE4yN9hTeQbOhnSR

-- Dumped from database version 16.13 (Debian 16.13-1.pgdg13+1)
-- Dumped by pg_dump version 16.13 (Debian 16.13-1.pgdg13+1)

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: booking_items; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.booking_items (
    id uuid NOT NULL,
    is_checked_in boolean NOT NULL,
    checked_in_at timestamp(6) without time zone,
    price_at_booking numeric(10,2) NOT NULL,
    qr_code_url text,
    qr_token character varying(255),
    qr_token_expires_at timestamp(6) without time zone,
    quantity integer NOT NULL,
    booking_id uuid NOT NULL,
    seat_inventory_id uuid,
    tier_id uuid NOT NULL
);


ALTER TABLE public.booking_items OWNER TO concert_user;

--
-- Name: bookings; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.bookings (
    id uuid NOT NULL,
    base_amount numeric(10,2) NOT NULL,
    booking_reference character varying(255) NOT NULL,
    cancellation_reason text,
    cancelled_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone,
    idempotency_key character varying(255),
    payment_gateway_fee numeric(10,2) NOT NULL,
    platform_fee numeric(10,2) NOT NULL,
    refund_amount numeric(10,2),
    status character varying(255) NOT NULL,
    total_amount numeric(10,2) NOT NULL,
    updated_at timestamp(6) without time zone,
    concert_id uuid NOT NULL,
    group_booking_id uuid,
    user_id uuid NOT NULL,
    CONSTRAINT bookings_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'CANCELLED'::character varying, 'EXPIRED'::character varying, 'ATTENDED'::character varying])::text[])))
);


ALTER TABLE public.bookings OWNER TO concert_user;

--
-- Name: concert_pre_registration; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.concert_pre_registration (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    has_purchased boolean NOT NULL,
    purchase_window_end timestamp(6) without time zone,
    purchase_window_start timestamp(6) without time zone,
    queue_position integer,
    concert_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.concert_pre_registration OWNER TO concert_user;

--
-- Name: concerts; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.concerts (
    id uuid NOT NULL,
    artist_name character varying(255) NOT NULL,
    banner_image_url text,
    event_category character varying(255),
    concert_date timestamp(6) without time zone NOT NULL,
    created_at timestamp(6) without time zone,
    description text NOT NULL,
    doors_open_time timestamp(6) without time zone,
    is_featured boolean NOT NULL,
    pre_registration_end timestamp(6) without time zone,
    pre_registration_start timestamp(6) without time zone,
    requires_pre_registration boolean NOT NULL,
    sale_end_time timestamp(6) without time zone,
    sale_start_time timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    organizer_id uuid NOT NULL,
    venue_id uuid NOT NULL,
    event_category_id uuid,
    CONSTRAINT concerts_event_category_check CHECK (((event_category)::text = ANY ((ARRAY['CONCERT_LIVE_MUSIC'::character varying, 'COMEDY_SHOW'::character varying, 'STANDUP_SPECIAL'::character varying, 'THEATRE_DRAMA'::character varying, 'FOOD_DRINK_FESTIVAL'::character varying, 'ART_EXHIBITION'::character varying, 'SPORTS_EVENT'::character varying, 'WORKSHOP_MASTERCLASS'::character varying, 'FILM_SCREENING'::character varying, 'CULTURAL_FESTIVAL'::character varying])::text[]))),
    CONSTRAINT concerts_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'SOLD_OUT'::character varying, 'CANCELLED'::character varying, 'POSTPONED'::character varying, 'COMPLETED'::character varying])::text[])))
);


ALTER TABLE public.concerts OWNER TO concert_user;

--
-- Name: event_categories; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.event_categories (
    id uuid NOT NULL,
    active boolean NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.event_categories OWNER TO concert_user;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO concert_user;

--
-- Name: group_bookings; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.group_bookings (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone NOT NULL,
    max_members integer NOT NULL,
    share_token character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    concert_id uuid NOT NULL,
    creator_id uuid NOT NULL,
    CONSTRAINT group_bookings_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'COMPLETED'::character varying, 'EXPIRED'::character varying, 'CANCELLED'::character varying])::text[])))
);


ALTER TABLE public.group_bookings OWNER TO concert_user;

--
-- Name: organizer_analytics; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.organizer_analytics (
    id uuid NOT NULL,
    attendance_rate double precision NOT NULL,
    average_rating double precision NOT NULL,
    checked_in_count bigint NOT NULL,
    organizer_id uuid NOT NULL,
    total_bookings bigint NOT NULL,
    total_events bigint NOT NULL,
    total_revenue numeric(15,2) NOT NULL,
    total_reviews bigint NOT NULL,
    total_tickets_sold bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    version bigint
);


ALTER TABLE public.organizer_analytics OWNER TO concert_user;

--
-- Name: organizer_reviews; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.organizer_reviews (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    rating integer NOT NULL,
    review_text text NOT NULL,
    would_attend_again boolean,
    concert_id uuid NOT NULL,
    organizer_id uuid NOT NULL,
    reviewer_id uuid NOT NULL,
    CONSTRAINT organizer_reviews_rating_check CHECK (((rating <= 5) AND (rating >= 1)))
);


ALTER TABLE public.organizer_reviews OWNER TO concert_user;

--
-- Name: payments; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.payments (
    id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    attempt_count integer NOT NULL,
    created_at timestamp(6) without time zone,
    failure_reason character varying(255),
    payment_method character varying(255),
    razorpay_order_id character varying(255),
    razorpay_payment_id character varying(255),
    razorpay_refund_id character varying(255),
    razorpay_signature character varying(255),
    refund_amount numeric(10,2),
    refunded_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    booking_id uuid NOT NULL,
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['INITIATED'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'PENDING'::character varying, 'REFUNDED'::character varying])::text[])))
);


ALTER TABLE public.payments OWNER TO concert_user;

--
-- Name: seat_inventory; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.seat_inventory (
    id uuid NOT NULL,
    locked_until timestamp(6) without time zone,
    row_label character varying(255),
    seat_number character varying(255),
    status character varying(255) NOT NULL,
    version bigint,
    locked_by_user_id uuid,
    tier_id uuid NOT NULL,
    CONSTRAINT seat_invetory_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'LOCKED'::character varying, 'BOOKED'::character varying])::text[])))
);


ALTER TABLE public.seat_inventory OWNER TO concert_user;

--
-- Name: ticket_tiers; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.ticket_tiers (
    id uuid NOT NULL,
    available_quantity integer NOT NULL,
    is_active boolean NOT NULL,
    lock_ttl_minutes integer NOT NULL,
    max_per_user integer NOT NULL,
    price numeric(10,2) NOT NULL,
    sale_end timestamp(6) without time zone,
    sale_start timestamp(6) without time zone,
    tier_name character varying(255) NOT NULL,
    tier_status character varying(255) NOT NULL,
    total_quantity integer NOT NULL,
    version bigint NOT NULL,
    concert_id uuid NOT NULL,
    section_id uuid NOT NULL,
    CONSTRAINT ticket_tiers_tier_status_check CHECK (((tier_status)::text = ANY ((ARRAY['UPCOMING'::character varying, 'ACTIVE'::character varying, 'SOLD_OUT'::character varying, 'PAUSED'::character varying, 'EXPIRED'::character varying])::text[])))
);


ALTER TABLE public.ticket_tiers OWNER TO concert_user;

--
-- Name: users; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    is_active boolean NOT NULL,
    created_at timestamp(6) without time zone,
    email character varying(255) NOT NULL,
    is_email_verification boolean NOT NULL,
    name character varying(255) NOT NULL,
    oauth_id character varying(255),
    oauth_provider character varying(255),
    password character varying(255),
    phone character varying(255),
    is_phone_verified boolean NOT NULL,
    role character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ORGANIZER'::character varying, 'ADMIN'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO concert_user;

--
-- Name: venue_section; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.venue_section (
    id uuid NOT NULL,
    color_hex character varying(255),
    height real,
    is_active boolean NOT NULL,
    name character varying(255) NOT NULL,
    section_type character varying(255) NOT NULL,
    total_capacity integer NOT NULL,
    width real,
    x_position real,
    y_position real,
    venue_id uuid NOT NULL,
    CONSTRAINT venue_section_section_type_check CHECK (((section_type)::text = ANY ((ARRAY['GA'::character varying, 'ASSIGNED'::character varying])::text[])))
);


ALTER TABLE public.venue_section OWNER TO concert_user;

--
-- Name: venues; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.venues (
    id uuid NOT NULL,
    address character varying(255) NOT NULL,
    city character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    google_maps_url character varying(255),
    is_active boolean NOT NULL,
    latitude double precision,
    layout_image_url character varying(255),
    location_description text,
    longitude double precision,
    name character varying(255) NOT NULL,
    total_capacity integer NOT NULL,
    updated_at timestamp(6) without time zone,
    venue_type character varying(255) NOT NULL,
    CONSTRAINT venues_venue_type_check CHECK (((venue_type)::text = ANY ((ARRAY['ESTABLISHED'::character varying, 'TEMPORARY'::character varying])::text[])))
);


ALTER TABLE public.venues OWNER TO concert_user;

--
-- Name: waitlist; Type: TABLE; Schema: public; Owner: concert_user
--

CREATE TABLE public.waitlist (
    id uuid NOT NULL,
    auto_book boolean NOT NULL,
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone,
    notified_at timestamp(6) without time zone,
    "position" integer,
    status character varying(255) NOT NULL,
    tier_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT waitlist_status_check CHECK (((status)::text = ANY ((ARRAY['WAITING'::character varying, 'NOTIFIED'::character varying, 'CONVERTED'::character varying, 'EXPIRED'::character varying, 'CANCELLED'::character varying])::text[])))
);


ALTER TABLE public.waitlist OWNER TO concert_user;

--
-- Name: booking_items booking_items_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.booking_items
    ADD CONSTRAINT booking_items_pkey PRIMARY KEY (id);


--
-- Name: bookings bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT bookings_pkey PRIMARY KEY (id);


--
-- Name: concert_pre_registration concert_pre_registration_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.concert_pre_registration
    ADD CONSTRAINT concert_pre_registration_pkey PRIMARY KEY (id);


--
-- Name: concerts concerts_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.concerts
    ADD CONSTRAINT concerts_pkey PRIMARY KEY (id);


--
-- Name: event_categories event_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.event_categories
    ADD CONSTRAINT event_categories_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: group_bookings group_bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.group_bookings
    ADD CONSTRAINT group_bookings_pkey PRIMARY KEY (id);


--
-- Name: organizer_analytics organizer_analytics_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.organizer_analytics
    ADD CONSTRAINT organizer_analytics_pkey PRIMARY KEY (id);


--
-- Name: organizer_reviews organizer_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.organizer_reviews
    ADD CONSTRAINT organizer_reviews_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: seat_inventory seat_invetory_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.seat_inventory
    ADD CONSTRAINT seat_invetory_pkey PRIMARY KEY (id);


--
-- Name: ticket_tiers ticket_tiers_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.ticket_tiers
    ADD CONSTRAINT ticket_tiers_pkey PRIMARY KEY (id);


--
-- Name: event_categories uk1et3muobyw9w9dur2ww8bvhh7; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.event_categories
    ADD CONSTRAINT uk1et3muobyw9w9dur2ww8bvhh7 UNIQUE (name);


--
-- Name: payments uk3h326otx9ko45mitb1ptj38bi; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT uk3h326otx9ko45mitb1ptj38bi UNIQUE (razorpay_payment_id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: organizer_analytics uk82js4li999uve211ahoyepot4; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.organizer_analytics
    ADD CONSTRAINT uk82js4li999uve211ahoyepot4 UNIQUE (organizer_id);


--
-- Name: concert_pre_registration uk_prereg_user_concert; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.concert_pre_registration
    ADD CONSTRAINT uk_prereg_user_concert UNIQUE (user_id, concert_id);


--
-- Name: organizer_reviews uk_user_concert_review; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.organizer_reviews
    ADD CONSTRAINT uk_user_concert_review UNIQUE (reviewer_id, concert_id);


--
-- Name: payments ukc3w49re3w3eiexjdnm9khcsd8; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT ukc3w49re3w3eiexjdnm9khcsd8 UNIQUE (razorpay_order_id);


--
-- Name: users ukdu5v5sr43g5bfnji4vb8hg5s3; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukdu5v5sr43g5bfnji4vb8hg5s3 UNIQUE (phone);


--
-- Name: bookings uke92mgyq35mdeo8gc1un2o6uk0; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT uke92mgyq35mdeo8gc1un2o6uk0 UNIQUE (booking_reference);


--
-- Name: payments uknuscjm6x127hkb15kcb8n56wo; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT uknuscjm6x127hkb15kcb8n56wo UNIQUE (booking_id);


--
-- Name: bookings ukqc7ays8yeraglxyak4mq5kb99; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT ukqc7ays8yeraglxyak4mq5kb99 UNIQUE (idempotency_key);


--
-- Name: group_bookings ukswdm5rgdb835qtrjgnf4i8653; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.group_bookings
    ADD CONSTRAINT ukswdm5rgdb835qtrjgnf4i8653 UNIQUE (share_token);


--
-- Name: waitlist uq_waitlist_user_tier; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.waitlist
    ADD CONSTRAINT uq_waitlist_user_tier UNIQUE (user_id, tier_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: venue_section venue_section_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.venue_section
    ADD CONSTRAINT venue_section_pkey PRIMARY KEY (id);


--
-- Name: venues venues_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.venues
    ADD CONSTRAINT venues_pkey PRIMARY KEY (id);


--
-- Name: waitlist waitlist_pkey; Type: CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.waitlist
    ADD CONSTRAINT waitlist_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_analytics_organizer; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_analytics_organizer ON public.organizer_analytics USING btree (organizer_id);


--
-- Name: idx_booking_concert; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_booking_concert ON public.bookings USING btree (concert_id);


--
-- Name: idx_booking_reference; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_booking_reference ON public.bookings USING btree (booking_reference);


--
-- Name: idx_booking_status; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_booking_status ON public.bookings USING btree (status);


--
-- Name: idx_booking_user; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_booking_user ON public.bookings USING btree (user_id);


--
-- Name: idx_concert_artist; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_concert_artist ON public.concerts USING btree (artist_name);


--
-- Name: idx_concert_date; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_concert_date ON public.concerts USING btree (concert_date);


--
-- Name: idx_concert_status; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_concert_status ON public.concerts USING btree (status);


--
-- Name: idx_group_concert; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_group_concert ON public.group_bookings USING btree (concert_id);


--
-- Name: idx_group_creator; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_group_creator ON public.group_bookings USING btree (creator_id);


--
-- Name: idx_group_share_token; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_group_share_token ON public.group_bookings USING btree (share_token);


--
-- Name: idx_item_booking; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_item_booking ON public.booking_items USING btree (booking_id);


--
-- Name: idx_item_tier; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_item_tier ON public.booking_items USING btree (tier_id);


--
-- Name: idx_payment_booking; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_payment_booking ON public.payments USING btree (booking_id);


--
-- Name: idx_payment_razorpay_order; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_payment_razorpay_order ON public.payments USING btree (razorpay_order_id);


--
-- Name: idx_payment_status; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_payment_status ON public.payments USING btree (status);


--
-- Name: idx_prereg_concert; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_prereg_concert ON public.concert_pre_registration USING btree (concert_id);


--
-- Name: idx_prereg_user; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_prereg_user ON public.concert_pre_registration USING btree (user_id);


--
-- Name: idx_review_concert; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_review_concert ON public.organizer_reviews USING btree (concert_id);


--
-- Name: idx_review_organizer; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_review_organizer ON public.organizer_reviews USING btree (organizer_id);


--
-- Name: idx_review_reviewer; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_review_reviewer ON public.organizer_reviews USING btree (reviewer_id);


--
-- Name: idx_seat_inventory; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_seat_inventory ON public.seat_inventory USING btree (tier_id, id);


--
-- Name: idx_seat_status; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_seat_status ON public.seat_inventory USING btree (status);


--
-- Name: idx_tier_concert; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_tier_concert ON public.ticket_tiers USING btree (concert_id);


--
-- Name: idx_user_email; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_user_email ON public.users USING btree (email);


--
-- Name: idx_user_phone; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_user_phone ON public.users USING btree (phone);


--
-- Name: idx_venue_city; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_venue_city ON public.venues USING btree (city);


--
-- Name: idx_venue_location; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_venue_location ON public.venues USING btree (latitude, longitude);


--
-- Name: idx_venue_type; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_venue_type ON public.venues USING btree (venue_type);


--
-- Name: idx_waitlist_position; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_waitlist_position ON public.waitlist USING btree ("position");


--
-- Name: idx_waitlist_tier; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_waitlist_tier ON public.waitlist USING btree (tier_id);


--
-- Name: idx_waitlist_user; Type: INDEX; Schema: public; Owner: concert_user
--

CREATE INDEX idx_waitlist_user ON public.waitlist USING btree (user_id);


--
-- Name: concerts fk1p0b09ekoc6mhgcntc5nl0f3e; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.concerts
    ADD CONSTRAINT fk1p0b09ekoc6mhgcntc5nl0f3e FOREIGN KEY (venue_id) REFERENCES public.venues(id);


--
-- Name: group_bookings fk1rg3pn50f2tkaevqsm9k36s0y; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.group_bookings
    ADD CONSTRAINT fk1rg3pn50f2tkaevqsm9k36s0y FOREIGN KEY (creator_id) REFERENCES public.users(id);


--
-- Name: venue_section fk2653s1utv4al6v166fwec2bdb; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.venue_section
    ADD CONSTRAINT fk2653s1utv4al6v166fwec2bdb FOREIGN KEY (venue_id) REFERENCES public.venues(id);


--
-- Name: organizer_reviews fk3geh38dlgpbvruii0uthlw9hy; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.organizer_reviews
    ADD CONSTRAINT fk3geh38dlgpbvruii0uthlw9hy FOREIGN KEY (reviewer_id) REFERENCES public.users(id);


--
-- Name: waitlist fk5m1071ccuqfgbgb8e7itv22x2; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.waitlist
    ADD CONSTRAINT fk5m1071ccuqfgbgb8e7itv22x2 FOREIGN KEY (tier_id) REFERENCES public.ticket_tiers(id);


--
-- Name: seat_inventory fk7frglmb6dht43ltx3xpyf02cw; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.seat_inventory
    ADD CONSTRAINT fk7frglmb6dht43ltx3xpyf02cw FOREIGN KEY (locked_by_user_id) REFERENCES public.users(id);


--
-- Name: group_bookings fk9733nyj2ujywxiguk56ak56v0; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.group_bookings
    ADD CONSTRAINT fk9733nyj2ujywxiguk56ak56v0 FOREIGN KEY (concert_id) REFERENCES public.concerts(id);


--
-- Name: concert_pre_registration fka52dvp1a77t911sy5oux0s1on; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.concert_pre_registration
    ADD CONSTRAINT fka52dvp1a77t911sy5oux0s1on FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: payments fkc52o2b1jkxttngufqp3t7jr3h; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fkc52o2b1jkxttngufqp3t7jr3h FOREIGN KEY (booking_id) REFERENCES public.bookings(id);


--
-- Name: waitlist fkc99hy864betkwt5pdekgfxkk1; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.waitlist
    ADD CONSTRAINT fkc99hy864betkwt5pdekgfxkk1 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: bookings fkdbrqb46qfddkkka33787k1i9g; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fkdbrqb46qfddkkka33787k1i9g FOREIGN KEY (group_booking_id) REFERENCES public.group_bookings(id);


--
-- Name: bookings fkeyog2oic85xg7hsu2je2lx3s6; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fkeyog2oic85xg7hsu2je2lx3s6 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: organizer_reviews fkfy5ed2pvsmadbb2iqegggn5i0; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.organizer_reviews
    ADD CONSTRAINT fkfy5ed2pvsmadbb2iqegggn5i0 FOREIGN KEY (concert_id) REFERENCES public.concerts(id);


--
-- Name: concert_pre_registration fkj1ewumgjsu01yapfjtlb7wve0; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.concert_pre_registration
    ADD CONSTRAINT fkj1ewumgjsu01yapfjtlb7wve0 FOREIGN KEY (concert_id) REFERENCES public.concerts(id);


--
-- Name: booking_items fkk402wr3f29vu1kes2wsw867sl; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.booking_items
    ADD CONSTRAINT fkk402wr3f29vu1kes2wsw867sl FOREIGN KEY (seat_inventory_id) REFERENCES public.seat_inventory(id);


--
-- Name: ticket_tiers fkl78kw3g8bknbq6v9f80pn83mv; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.ticket_tiers
    ADD CONSTRAINT fkl78kw3g8bknbq6v9f80pn83mv FOREIGN KEY (concert_id) REFERENCES public.concerts(id);


--
-- Name: concerts fkmecsek7pknws0j328b4p4p8bq; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.concerts
    ADD CONSTRAINT fkmecsek7pknws0j328b4p4p8bq FOREIGN KEY (organizer_id) REFERENCES public.users(id);


--
-- Name: seat_inventory fknm8hotb5wxyocsgo6vqw1icu2; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.seat_inventory
    ADD CONSTRAINT fknm8hotb5wxyocsgo6vqw1icu2 FOREIGN KEY (tier_id) REFERENCES public.ticket_tiers(id);


--
-- Name: concerts fko5c8a3bmxhbtyg5438v61sdn4; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.concerts
    ADD CONSTRAINT fko5c8a3bmxhbtyg5438v61sdn4 FOREIGN KEY (event_category_id) REFERENCES public.event_categories(id);


--
-- Name: ticket_tiers fkoc41pvyb3r2sruc4r0jw5jg38; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.ticket_tiers
    ADD CONSTRAINT fkoc41pvyb3r2sruc4r0jw5jg38 FOREIGN KEY (section_id) REFERENCES public.venue_section(id);


--
-- Name: bookings fkoi8xfy0qgoyfkb9gjn6ldibq4; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fkoi8xfy0qgoyfkb9gjn6ldibq4 FOREIGN KEY (concert_id) REFERENCES public.concerts(id);


--
-- Name: organizer_reviews fkonw9u0laa13s9qi01jpgw5l6g; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.organizer_reviews
    ADD CONSTRAINT fkonw9u0laa13s9qi01jpgw5l6g FOREIGN KEY (organizer_id) REFERENCES public.users(id);


--
-- Name: booking_items fkpsd0wimnktxcpw0g6p3ooje85; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.booking_items
    ADD CONSTRAINT fkpsd0wimnktxcpw0g6p3ooje85 FOREIGN KEY (tier_id) REFERENCES public.ticket_tiers(id);


--
-- Name: booking_items fkrw74irmyat5c39cnjkn02u99m; Type: FK CONSTRAINT; Schema: public; Owner: concert_user
--

ALTER TABLE ONLY public.booking_items
    ADD CONSTRAINT fkrw74irmyat5c39cnjkn02u99m FOREIGN KEY (booking_id) REFERENCES public.bookings(id);


--
-- PostgreSQL database dump complete
--

\unrestrict Xe9ANpAagQuYQH02ur9hUBeupyTZMRsA6CoDLgzNMQJswRMWE4yN9hTeQbOhnSR

