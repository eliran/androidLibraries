package com.threeplay.core;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by eliranbe on 5/21/16.
 */
public class SeekableInputStreamTest {

    private SeekableInputStream stream;

    @Before
    public void setup_input_and_stream(){
        stream = new SeekableInputStream(mockInput(100L));
    }

    @Test
    public void seekable_stream_from_RandomAccessInput(){
        assertThat(stream.availableBytes(), is(100L));
    }

    @Test
    public void availableBytes_returns_the_number_of_bytes_left_when_to_stream_end(){
        stream.seek(50L);
        assertThat(stream.availableBytes(), is(50L));
    }

    @Test
    public void high_bit_set_values(){
        stream = new SeekableInputStream(mockInput(100L, 0x80));
        assertThat(stream.next16LEbits(), is(0x8180));
        assertThat(stream.next16BEbits(), is(0x8283));
        assertThat(stream.next32LEbits(), is(0x87868584));
        assertThat(stream.next32BEbits(), is(0x88898a8b));
    }

    @Test
    public void seek_cannot_seek_beyond_availableBytes(){
        stream.seek(stream.availableBytes()+1);
        assertThat(stream.availableBytes(), is(0L));
    }

    @Test
    public void can_read_byte_by_byte(){
        assertThat(stream.nextByte(), is(0));
        assertThat(stream.nextByte(), is(1));
        assertThat(stream.nextByte(), is(2));
        stream.seek(50L);
        assertThat(stream.nextByte(), is(50));
        assertThat(stream.nextByte(), is(51));
    }

    @Test
    public void reading_a_byte_when_availableByte_is_zero_returns_neg_1(){
        stream.seek(stream.availableBytes());
        assertThat(stream.nextByte(), is(-1));
    }

    @Test
    public void skip(){
        stream.skip(10);
        assertThat(stream.nextByte(), is(10));
    }

    @Test
    public void skip_past_the_end_of_stream_returns_only_the_amount_skipped(){
        stream.seek(stream.availableBytes()-5);
        assertThat(stream.skip(10), is(5L));
    }

    @Test
    public void read_16LEbits(){
        assertThat(stream.next16LEbits(), is(256));
    }

    @Test
    public void read_16BEbits(){
        assertThat(stream.next16BEbits(), is(1));
    }

    @Test
    public void read_32LEbits(){
        assertThat(stream.next32LEbits(), is(0x03020100));
    }

    @Test
    public void read_32BEbits(){
        assertThat(stream.next32BEbits(), is(0x00010203));
    }

    @Test
    public void substream_can_be_created_that_only_give_access_to_specified_region(){
        SeekableInputStream substream = stream.substream(5,5);
        assertThat(substream.availableBytes(), is(5L));
        assertThat(substream.nextByte(), is(5));
        substream.skip(3);
        assertThat(substream.nextByte(), is(9));
        assertThat(substream.availableBytes(), is(0L));
    }

    @Test
    public void substream_seek_within_stream_limits(){
        SeekableInputStream substream = stream.substream(5, 5);
        substream.seek(0);
        assertThat(substream.nextByte(), is(5));
    }

    @Test
    public void substream_read_only_allowed_inside_stream_limits(){
        SeekableInputStream substream = stream.substream(5, 5);
        substream.seek(substream.availableBytes()+1);
        assertThat(substream.nextByte(), is(-1));
    }

    @Test
    public void stream_from_bytes(){
        SeekableInputStream stream = SeekableInputStream.fromBytes(new byte[]{1,2,3,4,5,6,7,8});
        assertThat(stream.availableBytes(), is(8L));
        assertThat(stream.nextByte(), is(1));
        stream.seek(7);
        assertThat(stream.nextByte(), is(8));
    }

    @Test
    public void rewind_returns_to_offset_0(){
        stream.seek(10);
        stream.rewind();
        assertThat(stream.availableBytes(), is(100L));
        assertThat(stream.nextByte(), is(0));
    }

    @Test
    public void substream_with_no_arguments_uses_current_offset_as_base_and_availaleBytes_as_length(){
        stream.skip(10);
        SeekableInputStream sub = stream.substream();
        sub.rewind();
        stream.rewind();
        assertThat(sub.availableBytes(), is(stream.availableBytes() - 10));
        assertThat(sub.nextByte(), is(10));
    }

    @Test
    public void substream_of_substeam_should_have_the_correct_base_offset(){
        stream.skip(10);
        SeekableInputStream sub1 = stream.substream();
        sub1.skip(10);
        SeekableInputStream sub2 = sub1.substream();
        sub2.rewind();
        stream.rewind();
        assertThat(sub2.availableBytes(), is(stream.availableBytes()-20));
        assertThat(sub2.nextByte(), is(20));
    }

    @Test
    public void stream_can_return_inputStream() throws java.io.IOException {
        InputStream istream = stream.getInputStream();
        assertThat(istream.available(), is((int)stream.availableBytes()));
        assertThat(istream.markSupported(), is(false));
        istream.skip(5);
        assertThat(istream.read(), is(5));
        istream.skip(2);
        assertThat(istream.read(), is(8));
        byte[] buf = new byte[5];
        assertThat(istream.read(buf), is(5));
        assertThat(arrayBytePrefix(buf, new byte[]{9,10,11,12,13}), is(true));
    }

    @Test
    public void stream_can_fill_buffer(){
        byte[] buf = new byte[5];
        assertThat(stream.nextBytes(buf), is(5));
        assertThat(arrayBytePrefix(buf, new byte[]{0,1,2,3,4}), is(true));
    }

    @Test
    public void stream_can_read_buffer_with_offset(){
        byte[] buf = new byte[]{0,0,0,0,0,0,0,0,0,0};

        assertThat(stream.nextBytes(buf, 5, 5), is(5));
        assertThat(arrayBytePrefix(buf, new byte[]{0,0,0,0,0,0,1,2,3,4}), is(true));
    }

    private boolean arrayBytePrefix(byte array[], byte... values){
        int count = Math.min(array.length, values.length);
        for ( int i = 0; i < count; ++i ) {
            if ( array[i] != values[i] ) return false;
        }
        return true;
    }

    private RandomAccessInput mockInput(final long size){
        return mockInput(size, 0);
    }
    private RandomAccessInput mockInput(final long size, final int byteOffset){
        return new RandomAccessInput() {
            @Override
            public int read(byte[] buf, int bufOffset, long inputOffset, int length) {
                long maxLength = size() - inputOffset;
                if ( maxLength <= 0 ) return -1;
                if ( length > maxLength ) length = (int)maxLength;
                if ( length > 0 ) {
                    int retLength = length;
                    while ( length-- > 0 ) {
                        buf[bufOffset++] = (byte)(byteOffset + inputOffset++);
                    }
                    return retLength;
                }
                return 0;
            }

            @Override
            public long size() {
                return size;
            }
        };
    }

}
