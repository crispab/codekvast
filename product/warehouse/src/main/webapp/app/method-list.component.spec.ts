import {describe, inject, beforeEach, beforeEachProviders} from '@angular/core/testing';
import {TestComponentBuilder} from '@angular/compiler/testing';
import {MethodListComponent} from './method-list.component';
import {setBaseTestProviders} from '@angular/core/testing';
import { TEST_BROWSER_PLATFORM_PROVIDERS, TEST_BROWSER_APPLICATION_PROVIDERS} from '@angular/platform-browser/testing';
setBaseTestProviders(TEST_BROWSER_PLATFORM_PROVIDERS, TEST_BROWSER_APPLICATION_PROVIDERS);

describe('MethodListComponent', () => {

    // TODO: make test work

    let tcb;

    //setup
    beforeEachProviders(() => [TestComponentBuilder, MethodListComponent]);

    beforeEach(inject([TestComponentBuilder], _tcb => {
        tcb = _tcb
    }));

    it('should render nothing for zero-valued timestamps', done => {
        tcb.createAsync(MethodListComponent).then(fixture => {
               let methodList = fixture.componentInstance, element = fixture.nativeElement;
               methodList.data = {
                   methods: [
                       {
                           signature: 'sig1',
                           lastInvokedAtMillis: 0,
                           collectedSinceMillis: 0,
                           collectedToMillis: 0,
                           collectedDays: 0
                       }

                   ]};
               fixture.detectChanges(); //trigger change detection
               expect(element.querySelector('h1').innerText).toBe('Hello World!');
               done();
           })
           .catch(e => done.fail(e));
    });
});
