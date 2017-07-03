import {Component, OnInit} from '@angular/core';
import {WarehouseService} from '../../services/warehouse.service';
import {StatusData} from '../../model/status/StatusData';
import {Settings} from '../../components/settings.model';
import {StateService} from '../../services/state.service';
import {AgePipe} from '../../pipes/age.pipe';
import {DatePipe} from '@angular/common';

@Component({
    selector: 'ck-collection-status',
    template: require('./collection-status.component.html'),
    providers: [AgePipe, DatePipe]
})

export class CollectionStatusComponent implements OnInit {
    data: StatusData;
    errorMessage: string;
    settings: Settings;

    constructor(private stateService: StateService, private warehouse: WarehouseService, private agePipe: AgePipe) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(Settings.KEY, () => new Settings());
        this.refresh();
    }

    refresh() {
        this.warehouse
            .getStatus()
            .subscribe(data => {
                this.data = data;
                this.errorMessage = undefined;
                // this.data.maxCollectionPeriodDays = 30;
            }, error => {
                this.data = undefined;
                this.errorMessage = error.statusText ? error.statusText : error;
            }, () => console.log('getStatus() complete'));
    }

    communicationFailure() {
        let now = this.agePipe.transform(new Date(), 'shortTime');
        return now + ': Communication failure'
    }

    percentOf(num: number, maxNum: number) {
        return Math.round(num * 100 / maxNum) + '%'
    }

    maxCollectedDays() {
        return this.data.maxCollectionPeriodDays < 0 ? 'no collection limit' : 'max=' + this.data.maxCollectionPeriodDays;
    }

    collectedDaysPercent() {
        return this.data.maxCollectionPeriodDays < 0 ? '' : this.percentOf(this.data.collectedDays, this.data.maxCollectionPeriodDays);
    }

    collectionResolution() {
        return this.agePipe.transform(new Date().getTime() - this.data.collectionResolutionSeconds * 1000, 'age');
    }

    percentClasses(num: number, maxNum: number) {
        let percent = Math.round(num * 100 / maxNum);
        let overflow = percent > 100;
        let warning = percent >= 90 && percent <= 100;

        return {
            'text-right': true,
            'bg-danger': overflow,
            'bg-warning': warning,
            'text-white': overflow || warning
        }
    }

}
