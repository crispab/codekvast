import {Component, OnDestroy, OnInit} from '@angular/core';
import {WarehouseService} from '../../services/warehouse.service';
import {Settings} from '../../components/settings.model';
import {AuthData, StateService} from '../../services/state.service';
import {AgePipe} from '../../pipes/age.pipe';
import {DatePipe} from '@angular/common';
import {CollectionStatusComponentState} from './collection-status.component.state';
import {Agent} from '../../model/status/Agent';

@Component({
    selector: 'ck-collection-status',
    template: require('./collection-status.component.html'),
    providers: [AgePipe, DatePipe]
})

export class CollectionStatusComponent implements OnInit, OnDestroy {
    settings: Settings;
    state: CollectionStatusComponentState;
    agentsLabel = 'agents';

    constructor(private stateService: StateService, private warehouse: WarehouseService, private agePipe: AgePipe) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(Settings.KEY, () => new Settings());
        this.state = this.stateService.getState(CollectionStatusComponentState.KEY,
            () => new CollectionStatusComponentState(this.agePipe, this.warehouse));
        this.state.init();
        this.stateService.getAuthData().subscribe((ad: AuthData) => {
            this.agentsLabel = ad && ad.source === 'heroku' ? 'dynos' : 'agents';
        })
    }

    ngOnDestroy(): void {
        this.state.destroy();
    }

    collectionResolution() {
        return this.agePipe.transform(new Date().getTime() - this.state.data.collectionResolutionSeconds * 1000, 'age');
    }

    toPercent(num: number, maxNum: number) {
        return Math.round(num * 100 / maxNum);
    }

    progressBarType(num: number, maxNum: number) {
        let percent = this.toPercent(num, maxNum);
        if (percent > 100) {
            return 'danger';
        }
        if (percent > 90) {
            return 'warning';
        }
        return 'info';
    }

    agentsProgressValue() {
        if (this.state.data.numLiveEnabledAgents === this.state.data.numLiveAgents) {
            return this.state.data.numLiveEnabledAgents + ' agents';
        }
        let disabled = this.state.data.numLiveAgents - this.state.data.numLiveEnabledAgents;
        return `${this.state.data.numLiveAgents} agents (${disabled} suspended)`
    }

    isTrialPeriod() {
        return this.state.data.maxCollectionPeriodDays > 0;
    }

    trialPeriodEndDate() {
        let daysInMillis = 24 * 60 * 60 * 1000;
        return new Date(this.state.data.collectedSinceMillis + this.state.data.maxCollectionPeriodDays * daysInMillis);
    }

    agentUploadExpectedAtClasses(agent: Agent) {
        let invisible = !agent.agentAlive;
        let overdue = !invisible && agent.nextPublicationExpectedAtMillis < new Date().getTime() - 30000;
        return {
            'invisible': invisible,
            'bg-warning': overdue,
            'text-white': overdue
        };
    }

    getComments(agent: Agent) {
        if (!agent.agentAlive) {
            return 'terminated';
        }

        if (!agent.agentLiveAndEnabled) {
            return 'suspended';
        }
        return '';
    }

    getAgentsLabel() {
        return this.agentsLabel;
    }
}
