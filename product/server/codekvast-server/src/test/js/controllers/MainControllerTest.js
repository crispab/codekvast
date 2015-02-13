'use strict';

describe('Controller: MainController', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var MainController,
        scope,
        now = Date.now();

    // Initialize the controller and a mock scope
    beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        MainController = $controller('MainController', {
            $scope: scope
        });
    }));

    it('should have an empty list of signatures', function () {
        expect(scope.signatures.length).toBe(0);
    });

    it('should have an undefined collectorStatus', function () {
        expect(scope.collectorStatus).toBeUndefined();
    });

    it('should have an undefined progress', function () {
        expect(scope.progress).toBeUndefined();
    });

    it('should calculate age', function () {
        expect(getAge(now, 0)).toBe("");
        expect(getAge(now, now)).toBe("0 min");
        expect(getAge(now, now - 29000)).toBe("0 min");
        expect(getAge(now, now - 30000)).toBe("1 min");
        expect(getAge(now, now - 31000)).toBe("1 min");
        expect(getAge(now, now - 59000)).toBe("1 min");
        expect(getAge(now, now - 61000)).toBe("1 min");
        expect(getAge(now, now - 119000)).toBe("2 min");
        expect(getAge(now, now - 120000)).toBe("2 min");
        expect(getAge(now, now - 121000)).toBe("2 min");
        expect(getAge(now, now - 59 * 60000)).toBe("59 min");
        expect(getAge(now, now - 60 * 60000)).toBe("1 hours");
        expect(getAge(now, now - 24 * 60 * 60000)).toBe("1 days");
        expect(getAge(now, now - 7 * 24 * 60 * 60000)).toBe("1 weeks");
    })

    it('should handle collectorStatusMessage', function () {
        // given
        var data = {
            body: JSON.stringify({
                collectionStartedAtMillis: now - 3 * 24 * 3600 * 1000,
                updateReceivedAtMillis: now - 61000,
                collectors: [
                    {
                        collectorStartedAtMillis: now - 3 * 24 * 3600 * 1000,
                        updateReceivedAtMillis: now - 120000
                    },
                    {
                        collectorStartedAtMillis: now - 5 * 24 * 3600 * 1000,
                        updateReceivedAtMillis: now - 180000
                    }
                ]
            })
        };

        // when
        onCollectorStatusMessage(data);
        updateAges();

        // then
        expect(scope.collectorStatus.collectionAge).toBe("3 days");
        expect(scope.collectorStatus.updateAge).toBe("1 min");
        expect(scope.collectorStatus.collectors[0].collectorAge).toBe("3 days");
        expect(scope.collectorStatus.collectors[0].updateAge).toBe("2 min");
        expect(scope.collectorStatus.collectors[1].collectorAge).toBe("5 days");
        expect(scope.collectorStatus.collectors[1].updateAge).toBe("3 min");
    });
});
