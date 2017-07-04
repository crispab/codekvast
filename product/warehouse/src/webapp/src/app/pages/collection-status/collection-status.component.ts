import {Component, OnDestroy, OnInit} from '@angular/core';
import {WarehouseService} from '../../services/warehouse.service';
import {Settings} from '../../components/settings.model';
import {StateService} from '../../services/state.service';
import {AgePipe} from '../../pipes/age.pipe';
import {DatePipe} from '@angular/common';
import {CollectionStatusComponentState} from './collection-status.component.state';
import {Agent} from '../../model/status/Agent';

@Component({
    selector: 'ck-collection-status',
    template: require('./collection-status.component.html'),
    styles: [require('./collection-status.component.css')],
    providers: [AgePipe, DatePipe]
})

export class CollectionStatusComponent implements OnInit, OnDestroy {
    settings: Settings;
    state: CollectionStatusComponentState;

    constructor(private stateService: StateService, private warehouse: WarehouseService, private agePipe: AgePipe) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(Settings.KEY, () => new Settings());
        this.state = this.stateService.getState(CollectionStatusComponentState.KEY,
            () => new CollectionStatusComponentState(this.agePipe, this.warehouse));
        this.state.init();
    }

    ngOnDestroy(): void {
        this.state.destroy();
    }

    collectionResolution() {
        return this.agePipe.transform(new Date().getTime() - this.state.data.collectionResolutionSeconds * 1000, 'age');
    }

    progressBarType(num: number, maxNum: number) {
        let percent = Math.round(num * 100 / maxNum);
        if (percent > 100) {
            return 'danger';
        }
        if (percent > 90) {
            return 'warning';
        }
        if (percent > 50) {
            return 'info';
        }
        return 'success';
    }

    agentsProgressValue() {
        if (this.state.data.numLiveEnabledAgents === this.state.data.numLiveAgents) {
            return this.state.data.numLiveEnabledAgents + ' agents';
        }
        let disabled = this.state.data.numLiveAgents - this.state.data.numLiveEnabledAgents;
        return `${this.state.data.numLiveAgents} agents (${disabled} disabled)`
    }

    isTrialPeriod() {
        return this.state.data.maxCollectionPeriodDays > 0;
    }

    trialPeriodProgress() {
        return Math.round(this.state.data.collectedDays * 100 / this.state.data.maxCollectionPeriodDays);
    }

    trialPeriodEndDate() {
        let daysInMillis = 24 * 60 * 60 * 1000;
        let result = new Date(this.state.data.collectedSinceMillis + this.state.data.maxCollectionPeriodDays * daysInMillis);
        return result;
    }

    agentExpectedAtClasses(agent: Agent) {
        let invisible = !agent.agentAlive;
        let overdue = !invisible && agent.nextPublicationExpectedAtMillis < new Date().getTime() - 30000;
        return {
            'invisible' : invisible,
            'bg-warning': overdue,
            'text-white': overdue
        };
    }

    getComments(agent: Agent) {
        console.log('Calculating comments for %o', agent);

        if (!agent.agentAlive) {
            return 'terminated';
        }

        if (!agent.agentLiveAndEnabled) {
            return 'suspended';
        }
        return '';
    }
}
