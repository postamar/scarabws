scarabws
========

A rewrite of http://www.kurokatta.org/hacks/scarab as a clojure web service.
Tests scrabble letter combinations.

## Usage

The webservice is currently hosted at `http://mariusposta.info/scarabws` for the moment:
- GET `http://mariusposta.info/scarabws/` for all available languages,
- GET `http://mariusposta.info/scarabws/<language>` for some basic statistics regarding `language`,
- GET `http://mariusposta.info/scarabws/<language>/<letter-query>` for exact- and sub-matches of the letters in `letter_query` for `language`. Lower case letters and '_' for blank tiles only. Append with `?exact` for exact matches only.

## Design

Before launching the webserver, scarabws looks in the `dictionnaries` subfolder and scans for _.txt_ files which contain a list of valid words, in lower case letters and separated by newlines. If no _.kv_ and _.edn_ files are present for a corresponding _.txt_ file, scarabws will build them. They respectively contain a key-value table and statistical information about the language. Each word is mapped to a key which, basically, corresponds to a re-ordering of the word's letters in decreasing order of frequency in its language. Searching for all matches to a query is somewhat complicated by the possibility of up to 2 blank tiles, and involves iterating over all possible keys which can be generated from the query. 

## License

Copyright © 2013 Marius Posta.

Distributed under the Eclipse Public License, the same as Clojure.
