import { InsertPageContentData, InsertPageContentVariables, GetPageContentByPathData, GetPageContentByPathVariables, UpdatePageContentData, UpdatePageContentVariables, DeletePageContentData, DeletePageContentVariables } from '../';
import { UseDataConnectQueryResult, useDataConnectQueryOptions, UseDataConnectMutationResult, useDataConnectMutationOptions} from '@tanstack-query-firebase/react/data-connect';
import { UseQueryResult, UseMutationResult} from '@tanstack/react-query';
import { DataConnect } from 'firebase/data-connect';
import { FirebaseError } from 'firebase/app';


export function useInsertPageContent(options?: useDataConnectMutationOptions<InsertPageContentData, FirebaseError, InsertPageContentVariables>): UseDataConnectMutationResult<InsertPageContentData, InsertPageContentVariables>;
export function useInsertPageContent(dc: DataConnect, options?: useDataConnectMutationOptions<InsertPageContentData, FirebaseError, InsertPageContentVariables>): UseDataConnectMutationResult<InsertPageContentData, InsertPageContentVariables>;

export function useGetPageContentByPath(vars: GetPageContentByPathVariables, options?: useDataConnectQueryOptions<GetPageContentByPathData>): UseDataConnectQueryResult<GetPageContentByPathData, GetPageContentByPathVariables>;
export function useGetPageContentByPath(dc: DataConnect, vars: GetPageContentByPathVariables, options?: useDataConnectQueryOptions<GetPageContentByPathData>): UseDataConnectQueryResult<GetPageContentByPathData, GetPageContentByPathVariables>;

export function useUpdatePageContent(options?: useDataConnectMutationOptions<UpdatePageContentData, FirebaseError, UpdatePageContentVariables>): UseDataConnectMutationResult<UpdatePageContentData, UpdatePageContentVariables>;
export function useUpdatePageContent(dc: DataConnect, options?: useDataConnectMutationOptions<UpdatePageContentData, FirebaseError, UpdatePageContentVariables>): UseDataConnectMutationResult<UpdatePageContentData, UpdatePageContentVariables>;

export function useDeletePageContent(options?: useDataConnectMutationOptions<DeletePageContentData, FirebaseError, DeletePageContentVariables>): UseDataConnectMutationResult<DeletePageContentData, DeletePageContentVariables>;
export function useDeletePageContent(dc: DataConnect, options?: useDataConnectMutationOptions<DeletePageContentData, FirebaseError, DeletePageContentVariables>): UseDataConnectMutationResult<DeletePageContentData, DeletePageContentVariables>;
