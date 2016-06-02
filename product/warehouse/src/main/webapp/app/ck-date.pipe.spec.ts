import {describe, expect, it, inject, beforeEach, beforeEachProviders} from 'angular2/testing';
import {DatePipe} from 'angular2/src/common/pipes/date_pipe';
import {CkDatePipe} from './ck-date.pipe';

describe('WarehouseService', () => {

    let pipe;
    let datePipe;
    let minutes = 60 * 1000;
    let hours = 60 * minutes;
    let days = 24 * hours;

    beforeEachProviders(() => [DatePipe]);

    beforeEach(inject([DatePipe], (_datePipe) => {
        datePipe = _datePipe;
        pipe = new CkDatePipe();
    }));

    it("Should return null for null", done => {
        expect(pipe.transform(null)).toBe(null);
        done();
    });

    it("Should return null for zero", done => {
        expect(pipe.transform(0)).toBe(null);
        done();
    });

    it("Should delegate to DatePipe when no pattern", done => {
        let value = new Date();
        expect(pipe.transform(value)).toBe(datePipe.transform(value));
        done();
    });

    it("Should delegate to DatePipe when unrecognized pattern", done => {
        let value = new Date();
        let pattern = 'shortDate'; // NOTE: not the default pattern 'mediumDate'
        expect(pipe.transform(value, pattern)).toBe(datePipe.transform(value, pattern));
        done();
    });

    it("Should recognize 'age' pattern", done => {
        let now = new Date().getTime();
        let value = new Date(now - 2 * days - 4 * hours - 36 * minutes);
        expect(pipe.transform(value, 'age')).toBe('2d 4h');
        done();
    });
});
