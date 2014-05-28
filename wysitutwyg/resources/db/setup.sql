PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS end_string;
DROP TABLE IF EXISTS parse;
DROP TABLE IF EXISTS corpus;

CREATE TABLE corpus (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
  text VARCHAR NOT NULL);

CREATE TABLE parse (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  corpus_id INTEGER NOT NULL,
  name VARCHAR NOT NULL,
  arity INTEGER NOT NULL,
  delimiter_regex VARCHAR NOT NULL,
  json VARCHAR NOT NULL,
  FOREIGN KEY(corpus_id) REFERENCES corpus(id));

CREATE TABLE end_string (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  parse_id INTEGER NOT NULL,
  string VARCHAR NOT NULL,
  FOREIGN KEY(parse_id) REFERENCES parse(id));