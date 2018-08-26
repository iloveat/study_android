#include <stdio.h>
#include <memory.h>
#include <stdlib.h>
#include <assert.h>
#include "src/emApiKernel.h"
//#include "model_a.h"
//#include "model_b.h"
//#include "model_c.h"
#include "model_d.h"
#include "parse_string.h"
#include <iostream>
#include <string>
#include "src/vocoder/common.h"
#include "src/vocoder/world.h"


using namespace std;

//#define emTTS_HEAP_SIZE	(64<<10)	//LSP堆内存的最低阀值				 (MGC的还会多出20K)
#define emTTS_HEAP_SIZE		(124<<10)	//LSP堆内存的最低阀值，一般要多出20K，(MGC的还会多出20K)
#define emTTS_CACHE_SIZE	(512)
#define emTTS_CACHE_COUNT	(64)
#define emTTS_CACHE_EXT		(4)

// 模型文件指针
char *pModelBuffer = NULL;
// 将音频数据发送出去
void dispatchAudioBuffer(char *buff, int buff_size);
// 全局停止TTS
emBool global_bStopTTS = emFalse;


// 加载模型参数到内存
void loadModel()
{
    if(pModelBuffer != NULL)
    {
        free(pModelBuffer);
        pModelBuffer = NULL;
    }
    string params = recoverFromString(str_bin);
    int len = params.length();
    pModelBuffer = (char*)malloc(sizeof(char)*(len+1)); //定义数组长度
    memcpy(pModelBuffer, params.c_str(), len);
    pModelBuffer[len] = '\0';
    params.clear();
}

//读取资源回调函数
void emCall CBReadRes(
    emPointer		pParameter,			//[in]用户资源回调指针
    emPointer		pBuffer,			//[out]读取资源存放的buffer
    emResAddress	iPos,				//[in]读取的起始地址
    emResSize		nSize )				//[in]读取的大小
{
    if(pModelBuffer != NULL)
    {
        // 从内存中直接读取模型参数
        memcpy(pBuffer, pModelBuffer+iPos, nSize);
    }
}

//音频输出回调函数
emTTSErrID CBOutputPCM( emPointer pParameter,   //[in] 用户在调用函数emTTS_Create时指定的第4个回调参数
        emUInt16		nCode,                  //[in] 输出音频数据的格式码
        emPByte			pcData,                 //[in] 输出音频数据缓冲区指针
        emSize			nSize )                 //[in] 音频数据长度（以字节为单位）
{
    if(global_bStopTTS == emFalse)
    {
        dispatchAudioBuffer((char*)pcData, nSize);
    }
    return emTTS_ERR_OK;
}

//进度回调函数
emTTSErrID CBGetProgress( emPointer pParameter,
                         emUInt32 iProcBegin,
                         emUInt32 nProcLen )
{
    return emTTS_ERR_OK;
}

void initTTS(emPByte &mpHeap, emTResPackDesc &mtResPackDesc)
{
    loadModel();
    assert(pModelBuffer != NULL);

    if(mpHeap != NULL)
    {
        free(mpHeap);
        mpHeap = NULL;
        if(mtResPackDesc.pCacheBlockIndex != NULL)
        {
            free(mtResPackDesc.pCacheBlockIndex);
            mtResPackDesc.pCacheBlockIndex = NULL;
        }
        if(mtResPackDesc.pCacheBuffer != NULL)
        {
            free(mtResPackDesc.pCacheBuffer);
            mtResPackDesc.pCacheBuffer = NULL;
        }
    }

    mpHeap = (emPByte)malloc(emTTS_HEAP_SIZE);
    memset(mpHeap, 0, emTTS_HEAP_SIZE);

    //mtResPackDesc.pCBParam = fopen(model_path, "rb");
    //为了初始化成功，必须强制指定一个非空的文件指针
    mtResPackDesc.pCBParam = (FILE*)0x1;
    assert(mtResPackDesc.pCBParam != NULL);
    mtResPackDesc.pfnRead = CBReadRes;
    mtResPackDesc.nSize = 0;
    mtResPackDesc.pCacheBlockIndex = (unsigned char*)malloc((emTTS_CACHE_COUNT+emTTS_CACHE_EXT)*sizeof(char));
    mtResPackDesc.pCacheBuffer = (unsigned char*)malloc((emTTS_CACHE_COUNT+emTTS_CACHE_EXT)*(emTTS_CACHE_SIZE)*sizeof(char));
    mtResPackDesc.nCacheBlockSize = emTTS_CACHE_SIZE;
    mtResPackDesc.nCacheBlockCount = emTTS_CACHE_COUNT;
    mtResPackDesc.nCacheBlockExt = emTTS_CACHE_EXT;
}

void synthAudio(emPByte &mpHeap, emTResPackDesc &mtResPackDesc, const char *text2synthesize)
{
    global_bStopTTS = emFalse;

    emHTTS mhTTS;
    emTTSErrID mReturn = emTTS_Create(&mhTTS, (emPointer)mpHeap, emTTS_HEAP_SIZE, NULL, (emPResPackDesc)&mtResPackDesc, (emSize)1);
    assert(mReturn == emTTS_ERR_OK);

    // 设置音频输出回调
    mReturn = emTTS_SetParam(mhTTS, emTTS_PARAM_OUTPUT_CALLBACK, (long unsigned int)CBOutputPCM);
    assert(mReturn == emTTS_ERR_OK);

    // 设置进度输出回调
    mReturn = emTTS_SetParam(mhTTS, emTTS_PARAM_PROGRESS_CALLBACK, (long unsigned int)CBGetProgress);
    assert(mReturn == emTTS_ERR_OK);

    mReturn = emTTS_SynthText(mhTTS, text2synthesize, -1);
    assert(mReturn == emTTS_ERR_OK);

    emTTS_Destroy(mhTTS);
}

void safeFree(emPByte &mpHeap, emTResPackDesc &mtResPackDesc)
{
    if(mtResPackDesc.pCacheBuffer != NULL)
    {
        free(mtResPackDesc.pCacheBuffer);
        mtResPackDesc.pCacheBuffer = NULL;
    }
    if(mtResPackDesc.pCacheBlockIndex != NULL)
    {
        free(mtResPackDesc.pCacheBlockIndex);
        mtResPackDesc.pCacheBlockIndex = NULL;
    }
    if(mpHeap != NULL)
    {
        free(mpHeap);
        mpHeap = NULL;
    }
    if(pModelBuffer != NULL)
    {
        free(pModelBuffer);
        pModelBuffer = NULL;
    }
}

void stopSynthesize()
{
    global_bStopTTS = emTrue;
}

void tts_vocoder(const char *params, const int &length)
{
    const int offline = 0;  //1:offline,0:online
    const int num_spectrum = 60;
    const int num_skip = 0;
    const double world_alpha = 0.58;

    int num_column = num_spectrum+1+1;  //mgc+lf0+bap
    int frame_size = sizeof(float)*num_column;
    int num_frame = length/frame_size;  // calculate frame number
    assert(length % frame_size == 0);

    float **world_input_param = (float**)malloc(num_frame*sizeof(float*));
    for(int i = 0; i < num_frame; i++)
    {
        world_input_param[i] = (float*)malloc(frame_size);
        for(int j = 0; j < num_spectrum; j++)  //mgc
        {
            world_input_param[i][j] = *((float*)(params+i*frame_size+j*(sizeof(float))));
        }
        world_input_param[i][num_spectrum] = *((float*)(params+i*frame_size+num_spectrum*(sizeof(float))));  //lf0
        world_input_param[i][num_spectrum+1] = *((float*)(params+i*frame_size+(num_spectrum+1)*(sizeof(float))));  //bap
    }

    int y_length = (int)((num_frame-num_skip*2)*5.0/1000*16000);
    double *y = (double *)malloc(sizeof(double)*y_length);

    HTS_World_Synthesize(world_input_param,
                         world_alpha,
                         num_spectrum,
                         num_frame,
                         0+num_skip,
                         num_frame-num_skip,
                         num_skip,
                         offline,
                         y_length,
                         y);

    for(int i = 0; i < num_frame; i++)
    {
        if(world_input_param[i])
        {
            free(world_input_param[i]);
            world_input_param[i] = NULL;
        }
    }
    if(world_input_param)
    {
        free(world_input_param);
        world_input_param = NULL;
    }

    short *pPCM = (short*)malloc(sizeof(short)*y_length);
    for(int i = 0; i < y_length; i++)
    {
        pPCM[i] = (short)(MyMaxInt(-32768, MyMinInt(32767, (int)(y[i]*32767))));
    }

    //dispatchAudioBuffer((char*)pPCM, y_length*2);
    FILE *fid = fopen("/sdcard/pcm_from_params.pcm", "wb+");
    fwrite((char*)pPCM, 1, sizeof(short)*y_length, fid);
    fclose(fid);

    if(pPCM)
    {
        free(pPCM);
        pPCM = NULL;
    }
    if(y)
    {
        free(y);
        y = NULL;
    }

/*
    const int offline = 0;  //1:offline,0:online
    const int num_spectrum = 59;
    const int num_skip = 0;
    const double world_alpha = 0.58;

    int num_column = num_spectrum+1+1;  //mgc+lf0+bap
    int frame_size = sizeof(float)*num_column;
    int num_frame = length/frame_size;  // calculate frame number
    assert(length % frame_size == 0);

    float **world_input_param = (float**)malloc(num_frame*sizeof(float*));
    for(int i = 0; i < num_frame; i++)
    {
        world_input_param[i] = (float*)malloc(frame_size);
        for(int j = 0; j < num_spectrum; j++)  //mgc
        {
            world_input_param[i][j] = *((float*)(params+i*frame_size+j*(sizeof(float))));
        }
        world_input_param[i][num_spectrum] = *((float*)(params+i*frame_size+num_spectrum*(sizeof(float))));  //lf0
        world_input_param[i][num_spectrum+1] = *((float*)(params+i*frame_size+(num_spectrum+1)*(sizeof(float))));  //bap
    }

    int y_length = (int)((num_frame-num_skip*2)*5.0/1000*16000);
    double *y = (double *)malloc(sizeof(double)*y_length);

    HTS_World_Synthesize(world_input_param,
                         world_alpha,
                         num_spectrum,
                         num_frame,
                         0+num_skip,
                         num_frame-num_skip,
                         num_skip,
                         offline,
                         y_length,
                         y);

    for(int i = 0; i < num_frame; i++)
    {
        if(world_input_param[i])
        {
            free(world_input_param[i]);
            world_input_param[i] = NULL;
        }
    }
    if(world_input_param)
    {
        free(world_input_param);
        world_input_param = NULL;
    }

    short *pPCM = (short*)malloc(sizeof(short)*y_length);
    for(int i = 0; i < y_length; i++)
    {
        pPCM[i] = (short)(MyMaxInt(-32768, MyMinInt(32767, (int)(y[i]*32767))));
    }

    dispatchAudioBuffer((char*)pPCM, y_length*2);

    if(pPCM)
    {
        free(pPCM);
        pPCM = NULL;
    }
    if(y)
    {
        free(y);
        y = NULL;
    }
*/
}

