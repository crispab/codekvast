import {Agent} from './agent';
import {Application} from './application';
import {Environment} from './environment';

export class StatusData {
    // query stuff
    timestamp: number;
    queryTimeMillis: number;

    // price plan stuff
    pricePlan: string;
    retentionPeriodDays: number;
    collectionResolutionSeconds: number;
    maxNumberOfAgents: number;
    maxNumberOfMethods: number;
    trialPeriodEndsAtMillis: number;

    // actual values
    numMethods: number;
    collectedSinceMillis: number;
    trialPeriodPercent: number;
    trialPeriodExpired: boolean;

    numAgents: number;
    numLiveAgents: number;
    numLiveEnabledAgents: number;

    // details
    environments: Environment[];
    applications: Application[];
    agents: Agent[];
}
