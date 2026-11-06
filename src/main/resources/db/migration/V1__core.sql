CREATE TABLE users
(
    id            uuid PRIMARY KEY,
    username      varchar(120) UNIQUE NOT NULL,
    password_hash varchar(255)        NOT NULL,
    created_at    timestamptz         NOT NULL DEFAULT now(),
    updated_at    timestamptz         NOT NULL DEFAULT now()
);
CREATE TABLE profiles
(
    id         uuid PRIMARY KEY,
    version    bigint      NOT NULL DEFAULT 0,
    data       jsonb       NOT NULL DEFAULT '{}',
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE contents
(
    id            uuid PRIMARY KEY,
    kind          varchar(40)  NOT NULL,
    title         varchar(240) NOT NULL,
    slug          varchar(180) NOT NULL,
    summary       text         NOT NULL DEFAULT '',
    body          jsonb        NOT NULL DEFAULT '{
      "type": "doc",
      "content": []
    }',
    data          jsonb        NOT NULL DEFAULT '{}',
    status        varchar(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    featured      boolean      NOT NULL DEFAULT false,
    display_order integer      NOT NULL DEFAULT 0,
    version       bigint       NOT NULL DEFAULT 0,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    published_at  timestamptz,
    UNIQUE (kind, slug)
);
CREATE INDEX content_public_idx ON contents (kind, status, display_order, published_at DESC);
CREATE INDEX content_search_idx ON contents USING gin (to_tsvector('english', title || ' ' || summary));
CREATE TABLE blogs
(
    id         uuid PRIMARY KEY,
    content_id uuid         NOT NULL REFERENCES contents (id) ON DELETE CASCADE,
    snapshot   jsonb        NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    actor      varchar(120) NOT NULL
);
CREATE INDEX content_idx ON blogs (content_id, created_at DESC);
CREATE TABLE audit_logs
(
    id          uuid PRIMARY KEY,
    action      varchar(100) NOT NULL,
    resource    varchar(100) NOT NULL,
    resource_id varchar(200),
    actor       varchar(120) NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX audit_created_idx ON audit_logs (created_at DESC);
