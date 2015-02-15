'use strict';

describe('DateService', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var dateService,
        now = Date.now();

    // Initialize the services to test
    beforeEach(inject(function (_dateService_) {
        dateService = _dateService_;
    }));

    it('should calculate age since', function () {
        expect(dateService.getAgeSince(now, 0)).toBe("");
        expect(dateService.getAgeSince(now, now)).toBe("");
        expect(dateService.getAgeSince(now, now - 29000)).toBe("29s");
        expect(dateService.getAgeSince(now, now - 30000)).toBe("30s");
        expect(dateService.getAgeSince(now, now - 31000)).toBe("31s");
        expect(dateService.getAgeSince(now, now - 59000)).toBe("59s");
        expect(dateService.getAgeSince(now, now - 60000)).toBe("1m");
        expect(dateService.getAgeSince(now, now - 61000)).toBe("1m 1s");
        expect(dateService.getAgeSince(now, now - 119000)).toBe("1m 59s");
        expect(dateService.getAgeSince(now, now - 120000)).toBe("2m");
        expect(dateService.getAgeSince(now, now - 121000)).toBe("2m 1s");
        expect(dateService.getAgeSince(now, now - 59 * 60000)).toBe("59m");
        expect(dateService.getAgeSince(now, now - 60 * 60000)).toBe("1h");
        expect(dateService.getAgeSince(now, now - 24 * 60 * 60000)).toBe("1d");
        expect(dateService.getAgeSince(now, now - 7 * 24 * 60 * 60000 - 120 * 60000 - 60000 - 30000)).toBe("7d 2h 1m 30s");
    })

    it('should calculate age since', function () {
        expect(dateService.getAge(0)).toBe("");
    })

});
