scarabws
========

A rewrite of http://www.kurokatta.org/hacks/scarab as a clojure web service.
Tests scrabble letter combinations.

## Usage

The webservice is currently hosted at http://scarabws.mariusposta.info for the moment:
- GET http://scarabws.mariusposta.info/ for all available languages,
- GET http://scarabws.mariusposta.info/<language> for some basic statistics regarding <language>,
- GET http://scarabws.mariusposta.info/<language>/<letter-query> for exact- and sub-matches of the letters in <letter_query> for <language>. Lower case letters and '_' for blank tiles only.


## License

Copyright © 2013 Marius Posta.

Distributed under the Eclipse Public License, the same as Clojure.
