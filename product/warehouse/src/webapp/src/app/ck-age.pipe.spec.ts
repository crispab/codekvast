import {DatePipe} from '@angular/common';
import {CkAgePipe} from './ck-age.pipe';

let pipe : CkAgePipe;
let parentPipe = new DatePipe("en_US");
let minutes = 60 * 1000;
let hours = 60 * minutes;
let days = 24 * hours;

describe('CkAgePipe', () => {

    beforeEach(() => {
        pipe = new CkAgePipe(parentPipe);
    });

    it("Should return null for null", done => {
        expect(pipe.transform(null)).toBe(null);
        done();
    });

    it("Should return null for zero", done => {
        expect(pipe.transform(0)).toBe(null);
        done();
    });

    it("Should delegate to parent DatePipe when no pattern", done => {
        let value = new Date();
        expect(pipe.transform(value)).toBe(parentPipe.transform(value));
        done();
    });

    it("Should delegate to parent DatePipe when unrecognized pattern", done => {
        let value = new Date();
        let pattern = 'shortDate'; // NOTE: not the default pattern 'mediumDate'
        expect(pipe.transform(value, pattern)).toBe(parentPipe.transform(value, pattern));
        done();
    });

    it("Should recognize 'age' pattern", done => {
        let now = new Date().getTime();
        let value = new Date(now - 2 * days - 4 * hours - 36 * minutes);
        expect(pipe.transform(value, 'age')).toBe('2d 4h 36m');
        done();
    });
});
