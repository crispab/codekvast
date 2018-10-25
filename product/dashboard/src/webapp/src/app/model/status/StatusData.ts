import {Agent} from './Agent';
import {Application} from './Application';
import {Environment} from './Environment';

export class StatusData {
    // query stuff
    timestamp: number;
    queryTimeMillis: number;

    // price plan stuff
    pricePlan: string;
    collectionResolutionSeconds: number;
    maxNumberOfAgents: number;
    maxNumberOfMethods: number;
    trialPeriodEndsAtMillis: number;

    // actual values
    numMethods: number;
    collectedSinceMillis: number;
    collectedDays: number;
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
