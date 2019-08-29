// Copyright (c) YugaByte, Inc.

import { VALIDATE_FROM_TOKEN, VALIDATE_FROM_TOKEN_RESPONSE,
         REGISTER, REGISTER_RESPONSE, LOGIN, LOGIN_RESPONSE, INSECURE_LOGIN, INSECURE_LOGIN_RESPONSE,
         INVALID_CUSTOMER_TOKEN, RESET_TOKEN_ERROR, RESET_CUSTOMER, LOGOUT, LOGOUT_SUCCESS,
         LOGOUT_FAILURE, FETCH_SOFTWARE_VERSIONS_FAILURE, FETCH_SOFTWARE_VERSIONS_SUCCESS,
         FETCH_SOFTWARE_VERSIONS, FETCH_TLS_CERTS, FETCH_TLS_CERTS_RESPONSE,
         ADD_TLS_CERT, ADD_TLS_CERT_RESPONSE, ADD_TLS_CERT_RESET, FETCH_HOST_INFO,
         FETCH_HOST_INFO_SUCCESS, FETCH_HOST_INFO_FAILURE, FETCH_CUSTOMER_COUNT,
         FETCH_YUGAWARE_VERSION, FETCH_YUGAWARE_VERSION_RESPONSE, UPDATE_PROFILE,
         UPDATE_PROFILE_SUCCESS, UPDATE_PROFILE_FAILURE, ADD_CUSTOMER_CONFIG,
         ADD_CUSTOMER_CONFIG_RESPONSE, FETCH_CUSTOMER_CONFIGS, FETCH_CUSTOMER_CONFIGS_RESPONSE,
         DELETE_CUSTOMER_CONFIG, DELETE_CUSTOMER_CONFIG_RESPONSE, GET_LOGS, GET_LOGS_SUCCESS,
         GET_LOGS_FAILURE, GET_RELEASES, GET_RELEASES_RESPONSE, REFRESH_RELEASES,
         REFRESH_RELEASES_RESPONSE, IMPORT_RELEASE, IMPORT_RELEASE_RESPONSE, UPDATE_RELEASE,
         UPDATE_RELEASE_RESPONSE, GET_ALERTS, GET_ALERTS_SUCCESS, GET_ALERTS_FAILURE
       } from '../actions/customers';

import { sortVersionStrings, isDefinedNotNull } from '../utils/ObjectUtils';
import { getInitialState, setLoadingState, setSuccessState, setFailureState, setPromiseResponse }  from '../utils/PromiseUtils';

import ossConfig from './configs/ossConfig';

const INITIAL_STATE = {
  currentCustomer: getInitialState({}),
  authToken: getInitialState({}),
  tasks: [],
  status: null,
  error: null,
  loading: false,
  softwareVersions: [],
  alertsList: [],
  hostInfo: null,
  customerCount: {},
  yugawareVersion: getInitialState({}),
  profile: getInitialState({}),
  addConfig: getInitialState({}),
  configs: getInitialState([]),
  deleteConfig: getInitialState({}),
  releases: getInitialState([]),
  refreshReleases: getInitialState({}),
  importRelease: getInitialState({}),
  updateRelease: getInitialState({}),
  addCertificate: getInitialState({}),
  userCertificates: getInitialState({}),
};

export default function(state = INITIAL_STATE, action) {
  switch(action.type) {
    case VALIDATE_FROM_TOKEN:
      return setLoadingState(state, "currentCustomer", {});
    case VALIDATE_FROM_TOKEN_RESPONSE:
      return setPromiseResponse(state, "currentCustomer", action);

    case REGISTER:
      return setLoadingState(state, "authToken", {});
    case REGISTER_RESPONSE:
      return setPromiseResponse(state, "authToken", action);

    case LOGIN:
      return setLoadingState(state, "authToken", {});
    case LOGIN_RESPONSE:
      return setPromiseResponse(state, "authToken", action);

    case INSECURE_LOGIN:
      return {
        ...state,
        INSECURE_apiToken: null,
      };
    case INSECURE_LOGIN_RESPONSE:
      const currentCustomer = {
        ...state.currentCustomer,
        data: {
          ...state.currentCustomer.data,
          features: ossConfig.features
        }
      };
      return {
        ...state,
        currentCustomer,
        INSECURE_apiToken: action.payload.data.apiToken
      };
    case LOGOUT:
      return {...state};
    case LOGOUT_SUCCESS:
      return {...state, currentCustomer: getInitialState({}), authToken: getInitialState({})};
    case LOGOUT_FAILURE:
      return {...state};
    case INVALID_CUSTOMER_TOKEN:
      return {...state, error: 'Invalid'};
    case RESET_TOKEN_ERROR:
      return {...state, error: null};
    case RESET_CUSTOMER:
      return {...state, currentCustomer: getInitialState({}), authToken: getInitialState({})};
    case FETCH_SOFTWARE_VERSIONS:
      return {...state, softwareVersions: []};
    case FETCH_SOFTWARE_VERSIONS_SUCCESS:
      return {...state, softwareVersions: sortVersionStrings(action.payload.data)};
    case FETCH_SOFTWARE_VERSIONS_FAILURE:
      return {...state};
    case FETCH_TLS_CERTS:
      return setLoadingState(state, "userCertificates", []);
    case FETCH_TLS_CERTS_RESPONSE:
      return setPromiseResponse(state, "userCertificates", action);
    case ADD_TLS_CERT:
      return setLoadingState(state, "addCertificate", {});
    case ADD_TLS_CERT_RESPONSE:
      if (action.payload.status !== 200) {
        if (isDefinedNotNull(action.payload.data)) {
          return setFailureState(state, "addCertificate", action.payload.data.error);
        } else {
          return state;
        }
      }
      return setPromiseResponse(state, "addCertificate", action);
    case ADD_TLS_CERT_RESET:
      return setLoadingState(state, "addCertificate", getInitialState({}));
    case FETCH_HOST_INFO:
      return {...state, hostInfo: null};
    case FETCH_HOST_INFO_SUCCESS:
      return {...state, hostInfo: action.payload.data};
    case FETCH_HOST_INFO_FAILURE:
      return {...state, hostInfo: null };

    case UPDATE_PROFILE:
      return setLoadingState(state, "profile");
    case UPDATE_PROFILE_SUCCESS:
      return setSuccessState(state, "profile", "updated-success");
    case UPDATE_PROFILE_FAILURE:
      return setFailureState(state, "profile", action.payload.data.error);
    case FETCH_CUSTOMER_COUNT:
      return setLoadingState(state, "customerCount");
    case GET_ALERTS:
      return {...state, alertsList: []};
    case GET_ALERTS_SUCCESS:
      return {...state, alertsList: action.payload.data};
    case GET_ALERTS_FAILURE:
      return {...state, alertsList: []};
    case FETCH_YUGAWARE_VERSION:
      return setLoadingState(state, "yugawareVersion", {});
    case FETCH_YUGAWARE_VERSION_RESPONSE:
      return setPromiseResponse(state, "yugawareVersion", action);
    case ADD_CUSTOMER_CONFIG:
      return setLoadingState(state, "addConfig", {});
    case ADD_CUSTOMER_CONFIG_RESPONSE:
      return setPromiseResponse(state, "addConfig", action);
    case FETCH_CUSTOMER_CONFIGS:
      return setLoadingState(state, "configs", []);
    case FETCH_CUSTOMER_CONFIGS_RESPONSE:
      return setPromiseResponse(state, "configs", action);
    case DELETE_CUSTOMER_CONFIG:
      return setLoadingState(state, "deleteConfig", {});
    case DELETE_CUSTOMER_CONFIG_RESPONSE:
      return setPromiseResponse(state, "deleteConfig", action);

    case GET_LOGS:
      return {...state, yugaware_logs: null};
    case GET_LOGS_SUCCESS:
      return {...state, yugaware_logs: action.payload.data.lines.reverse()};
    case GET_LOGS_FAILURE:
      return {...state, yugaware_logs: null };
    case GET_RELEASES:
      return setLoadingState(state, "releases", []);
    case GET_RELEASES_RESPONSE:
      return setPromiseResponse(state, "releases", action);
    case REFRESH_RELEASES:
      return setLoadingState(state, "refreshReleases", {});
    case REFRESH_RELEASES_RESPONSE:
      return setPromiseResponse(state, "refreshReleases", action);
    case IMPORT_RELEASE:
      return setLoadingState(state, "importRelease", {});
    case IMPORT_RELEASE_RESPONSE:
      return setPromiseResponse(state, "importRelease", action);
    case UPDATE_RELEASE:
      return setLoadingState(state, "updateRelease", {});
    case UPDATE_RELEASE_RESPONSE:
      return setPromiseResponse(state, "updateRelease", action);

    default:
      return state;
  }
}
