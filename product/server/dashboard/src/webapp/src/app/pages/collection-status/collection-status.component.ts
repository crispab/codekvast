import {Agent} from '../../model/status/agent';
import {AgePipe} from '../../pipes/age.pipe';
import {AuthData, StateService} from '../../services/state.service';
import {ClientSettings} from '../../model/client-settings';
import {CollectionStatusComponentState} from './collection-status.component.state';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {DatePipe} from '@angular/common';

@Component({
    selector: 'app-collection-status',
    templateUrl: './collection-status.component.html',
    providers: [AgePipe, DatePipe]
})

export class CollectionStatusComponent implements OnInit, OnDestroy {
    settings: ClientSettings;
    state: CollectionStatusComponentState;
    agentsLabel = 'Agents';

    constructor(private stateService: StateService, private api: DashboardApiService, private agePipe: AgePipe) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(ClientSettings.KEY, () => new ClientSettings());
        this.state = this.stateService.getState(CollectionStatusComponentState.KEY,
            () => new CollectionStatusComponentState(this.agePipe, this.api, this.stateService));
        this.state.init();
        this.stateService.getAuthData().subscribe((ad: AuthData) => {
            this.agentsLabel = ad && ad.source === 'heroku' ? 'Dynos' : 'Agents';
        });
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


    progressBarType2(num: number, maxNum: number) {
        let percent = this.toPercent(num, maxNum);
        return this.progressBarType(percent);
    }

    progressBarType(percent: number) {
        if (percent >= 100) {
            return 'danger';
        }
        if (percent > 90) {
            return 'warning';
        }
        return 'info';
    }

    trialPeriodProgressText(percent: number) {
        return percent < 100 ? `${percent}%` : 'Expired';
    }

    agentsProgressValue() {
        if (this.state.data.numLiveEnabledAgents === this.state.data.numLiveAgents) {
            return `${this.state.data.numLiveEnabledAgents} ${this.getAgentsLabel()}`;
        }
        let disabled = this.state.data.numLiveAgents - this.state.data.numLiveEnabledAgents;
        return `${this.state.data.numLiveAgents} ${this.getAgentsLabel()} (${disabled} suspended)`;
    }

    agentUploadExpectedAtClasses(agent: Agent) {
        let invisible = !agent.agentAlive;
        let overdue = !invisible && agent.nextPublicationExpectedAtMillis < new Date().getTime() - 30000;
        return {
            invisible: invisible,
            'bg-warning': overdue,
            'text-white': overdue
        };
    }

    getComments(agent: Agent) {
        if (agent.deletionState === 1) {
            return 'Deleting...';
        }

        if (agent.deletionState === 2) {
            return 'Deleted';
        }

        if (!agent.agentAlive) {
            return 'Terminated';
        }

        if (!agent.agentLiveAndEnabled) {
            return 'Suspended';
        }
        return '';
    }

    commentClasses(agent: Agent) {
        return {
            invisible: agent.deletionState !== 1,
            far: agent.deletionState === 1,
            'fa-clock': agent.deletionState === 1
        };
    }

    getAgentsLabel() {
        return this.agentsLabel;
    }

    isAgentDeletable(agent: Agent) {
        console.log('[ck dashboard] isDeletable(%o)', agent);
        return !agent.agentAlive && (agent.deletionState === null || agent.deletionState === undefined);
    }

}
